package com.jacuum.game;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages SSE emitters for active game sessions.
 * Runs each game loop in a virtual thread (Java 21).
 */
@Service
public class SseGameService {

    /** Speed-level to millisecond delay mapping (1=slowest, 5=fastest). */
    private static final int[] SPEED_DELAYS = {800, 400, 200, 80, 20};

    private final GameEngine engine;
    private final Map<String, Integer> speedLevels = new ConcurrentHashMap<>();

    public SseGameService(GameEngine engine) {
        this.engine = engine;
    }

    /**
     * Create an SSE emitter for a session and start the game loop in a virtual thread.
     */
    public SseEmitter stream(String sessionId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        speedLevels.put(sessionId, 3); // default speed level

        Thread.ofVirtual().name("game-loop-" + sessionId).start(() -> runLoop(sessionId, emitter));

        return emitter;
    }

    public void setSpeed(String sessionId, int level) {
        int clamped = Math.max(1, Math.min(5, level));
        speedLevels.put(sessionId, clamped);
    }

    // --- private ---

    private void runLoop(String sessionId, SseEmitter emitter) {
        try {
            while (true) {
                GameSession session = engine.getSession(sessionId);

                if (session.getStatus() == GameStatus.PAUSED) {
                    Thread.sleep(100);
                    continue;
                }

                if (!session.isActive()) {
                    // Terminal state — send final event and close
                    StepResult finalResult = buildSnapshot(session);
                    send(emitter, "step", finalResult);
                    emitter.complete();
                    return;
                }

                StepResult result = engine.step(sessionId);
                send(emitter, "step", result);

                if (!session.isActive()) {
                    emitter.complete();
                    return;
                }

                int delay = SPEED_DELAYS[speedLevels.getOrDefault(sessionId, 3) - 1];
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            emitter.completeWithError(e);
        } catch (Exception e) {
            emitter.completeWithError(e);
        } finally {
            speedLevels.remove(sessionId);
        }
    }

    private StepResult buildSnapshot(GameSession s) {
        return new StepResult(
            s.getRobotX(), s.getRobotY(), false,
            s.getMap().countCleanedTiles(), s.getMap().countFloorTiles(),
            s.getIterationsUsed(), s.getMaxIterations(),
            s.getScore(), s.getStatus()
        );
    }

    private void send(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(event).data(data));
    }
}
