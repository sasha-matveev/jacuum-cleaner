package com.jacuum.engine;

import com.jacuum.algo.Algorithms;
import com.jacuum.algo.Direction;
import com.jacuum.algo.RobotAlgo;
import com.jacuum.map.GameMap;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MemorySessions implements Sessions {

    private final ConcurrentHashMap<String, ActiveSession> store = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messaging;
    private final Algorithms algorithms;

    public MemorySessions(SimpMessagingTemplate messaging, Algorithms algorithms) {
        this.messaging = messaging;
        this.algorithms = algorithms;
    }

    @Override
    public String open(GameMap map, String algoName, String username,
                       String avatar, int iterations) throws Exception {
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
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
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
        if (messaging != null)
            messaging.convertAndSend("/topic/session/" + s.id + "/status",
                new StatusEvent(s.id, RunStatus.FINISHED, reason));
    }

    private void publish(String sessionId, IterationEvent event) {
        if (messaging != null)
            messaging.convertAndSend("/topic/session/" + sessionId + "/events", event);
    }

    @Override public void pause(String id)  throws Exception { require(id).status = RunStatus.PAUSED; }
    @Override public void resume(String id) throws Exception { require(id).status = RunStatus.RUNNING; }
    @Override public void stop(final String id) throws Exception {
        final ActiveSession s = require(id);
        synchronized (s) {
            if (s.status == RunStatus.FINISHED) return;
            finish(s, FinishReason.INTERRUPTED);
        }
        if (s.future != null) s.future.interrupt();
    }

    // package-private: used by the game loop (added in Task 8) within the engine package
    ActiveSession require(String id) throws Exception {
        ActiveSession s = store.get(id);
        if (s == null) throw new Exception("Unknown session: " + id);
        return s;
    }
}
