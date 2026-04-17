package com.jacuum.engine;

import com.jacuum.algo.*;
import com.jacuum.map.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GameLoopTest {

    // Minimal Algorithms that always returns a fixed algo
    private static Algorithms eastAlways() {
        return new Algorithms() {
            @Override public java.util.List<String> names() { return java.util.List.of("east"); }
            @Override public RobotAlgo instantiate(String name) { return tile -> Direction.EAST; }
        };
    }

    // 1×5 corridor: floor at y=1, x=1..5
    private static GameMap corridor() {
        boolean[][] f = new boolean[3][7];
        for (int x = 1; x < 6; x++) f[1][x] = true;
        return new GeneratedMap("corridor", SizePreset.TINY, f, 1, 1);
    }

    @Test void robotCleansCorridorMovingEast() throws Exception {
        MemorySessions sessions = new MemorySessions(null, eastAlways());
        String id = sessions.open(corridor(), "east", "Bot", "🤖", 20);
        sessions.start(id);

        // Wait for finish (max 2s)
        long deadline = System.currentTimeMillis() + 2000;
        while (sessions.view(id).status() != RunStatus.FINISHED
               && System.currentTimeMillis() < deadline)
            Thread.sleep(50);

        SessionView view = sessions.view(id);
        assertThat(view.status()).isEqualTo(RunStatus.FINISHED);
        assertThat(view.totalCleaned()).isGreaterThan(0);
        assertThat(view.score()).isGreaterThan(0);
    }

    @Test void algoCrashFinishesWithZeroScore() throws Exception {
        Algorithms crashAlgo = new Algorithms() {
            @Override public java.util.List<String> names() { return java.util.List.of("crash"); }
            @Override public RobotAlgo instantiate(String n) {
                return tile -> { throw new RuntimeException("boom"); };
            }
        };
        MemorySessions sessions = new MemorySessions(null, crashAlgo);
        String id = sessions.open(corridor(), "crash", "Bot", "🤖", 20);
        sessions.start(id);
        long deadline = System.currentTimeMillis() + 2000;
        while (sessions.view(id).status() != RunStatus.FINISHED
               && System.currentTimeMillis() < deadline)
            Thread.sleep(50);
        SessionView view = sessions.view(id);
        assertThat(view.finishReason()).isEqualTo(FinishReason.ALGO_CRASH);
    }
}
