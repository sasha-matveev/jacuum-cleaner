package com.jacuum.engine;

import com.jacuum.algo.Algorithms;
import com.jacuum.algo.Direction;
import com.jacuum.algo.RobotAlgo;
import com.jacuum.map.GameMap;
import org.springframework.beans.factory.annotation.Value;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MemorySessions implements Sessions {

    private final ConcurrentHashMap<String, ActiveSession> store = new ConcurrentHashMap<>();
    private final Messaging messaging;
    private final Algorithms algorithms;
    private final int maxSessions;

    public MemorySessions(final Messaging messaging, final Algorithms algorithms,
                         @Value("${game.max-sessions:50}") final int maxSessions) {
        this.messaging = messaging;
        this.algorithms = algorithms;
        this.maxSessions = maxSessions;
    }

    @Override
    public String open(GameMap map, String algoName, String username,
                       String avatar, int iterations) throws Exception {
        if (store.size() >= maxSessions) {
            throw new Exception("Session cap reached (" + maxSessions + " max)");
        }
        String id = UUID.randomUUID().toString();
        store.put(id, new ActiveSession(id, map, algoName, username, avatar, iterations));
        return id;
    }

    @Override
    public SessionView view(String id) throws Exception {
        ActiveSession s = require(id);
        return s.toView();
    }

    @Override
    public void start(String id) throws Exception {
        ActiveSession s = require(id);
        synchronized (s) {
            if (s.status != RunStatus.SETUP && s.status != RunStatus.PAUSED)
                throw new Exception("Cannot start session in state: " + s.status);
            s.status = RunStatus.RUNNING;
        }
        RobotAlgo algo = algorithms.instantiate(s.algoName);
        s.future = Thread.ofVirtual().start(() -> runLoop(s, algo));
    }

    private void runLoop(ActiveSession s, RobotAlgo algo) {
        while (s.iterationsUsed < s.iterationsAvailable && s.status != RunStatus.FINISHED) {
            if (s.status == RunStatus.PAUSED) {
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                continue;
            }
            SessionTile tile = new SessionTile(s.robotX, s.robotY, s.map, s.cleaned);
            Direction dir;
            try {
                dir = algo.next(tile);
            } catch (Exception e) {
                finish(s, FinishReason.ALGO_CRASH);
                return;
            }
            boolean moved = !s.map.hasWall(s.robotX, s.robotY, dir);
            if (moved) {
                s.robotX = s.robotX + dir.dx();
                s.robotY = s.robotY + dir.dy();
            }
            String key = s.robotX + "," + s.robotY;
            if (!s.cleaned.contains(key)) {
                s.cleaned.add(key);
                // Note: robotX, robotY, score, and iterationsUsed are individually volatile fields,
                // but are written sequentially without atomicity. This is safe because:
                // 1. The game loop thread is the sole writer of these fields
                // 2. score += 100 is a non-atomic read-modify-write, but safe due to single-writer guarantee
                // 3. The HTTP-thread reader (view()) may observe torn positions but this is acceptable for display
                s.score += 100;
            }
            s.iterationsUsed++;
            IterationEvent event = new IterationEvent(
                s.id, s.iterationsUsed, moved ? dir : null,
                s.robotX, s.robotY, s.score,
                s.cleaned.size(), s.map.totalFloorTiles(),
                false, null);
            publish(s.id, event);

            if (s.cleaned.size() == s.map.totalFloorTiles()) {
                finish(s, FinishReason.COMPLETED);
                return;
            }
        }
        if (s.status != RunStatus.FINISHED) finish(s, FinishReason.OUT_OF_ITERATIONS);
    }

    private void finish(ActiveSession s, FinishReason reason) {
        s.status = RunStatus.FINISHED;
        s.finishReason = reason;
        if (reason == FinishReason.ALGO_CRASH) s.score = 0;
        IterationEvent event = new IterationEvent(
            s.id, s.iterationsUsed, null,
            s.robotX, s.robotY, s.score,
            s.cleaned.size(), s.map.totalFloorTiles(),
            true, reason);
        publish(s.id, event);
        messaging.send("/topic/session/" + s.id + "/status",
            new StatusEvent(s.id, RunStatus.FINISHED, reason));
    }

    private void publish(final String sessionId, final IterationEvent event) {
        messaging.send("/topic/session/" + sessionId + "/events", event);
    }

    @Override public void pause(String id) throws Exception {
        ActiveSession s = require(id);
        synchronized (s) {
            if (s.status != RunStatus.RUNNING)
                throw new Exception("Cannot pause session in state: " + s.status);
            s.status = RunStatus.PAUSED;
        }
    }

    @Override public void resume(String id) throws Exception {
        ActiveSession s = require(id);
        synchronized (s) {
            if (s.status != RunStatus.PAUSED)
                throw new Exception("Cannot resume session in state: " + s.status);
            s.status = RunStatus.RUNNING;
        }
    }

    @Override public void stop(final String id) throws Exception {
        final ActiveSession s = require(id);
        synchronized (s) {
            if (s.status == RunStatus.FINISHED) return;
            finish(s, FinishReason.INTERRUPTED);
        }
        s.future.interrupt();
    }

    // package-private: used by the game loop (added in Task 8) within the engine package
    ActiveSession require(final String id) throws Exception {
        return Optional.ofNullable(store.get(id))
            .orElseThrow(() -> new Exception("Unknown session: " + id));
    }
}
