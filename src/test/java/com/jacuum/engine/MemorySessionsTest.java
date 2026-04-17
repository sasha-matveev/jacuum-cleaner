package com.jacuum.engine;

import com.jacuum.algo.*;
import com.jacuum.map.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MemorySessionsTest {

    // 3×3 all-floor map, start at (1,1)
    private static GameMap smallMap() {
        boolean[][] f = new boolean[3][3];
        for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) f[y][x] = true;
        return new GeneratedMap("test", SizePreset.TINY, f, 1, 1);
    }

    @Test void openCreatesSessionInSetupState() throws Exception {
        Sessions sessions = new MemorySessions(null, null, 50); // no messaging in unit test
        String id = sessions.open(smallMap(), "RandomAlgo", "Alice", "🤖", 100);
        assertThat(id).isNotBlank();
        SessionView view = sessions.view(id);
        assertThat(view.status()).isEqualTo(RunStatus.SETUP);
        assertThat(view.robotX()).isEqualTo(1);
        assertThat(view.robotY()).isEqualTo(1);
        assertThat(view.score()).isEqualTo(0);
        assertThat(view.totalFloor()).isEqualTo(9);
    }

    @Test void viewThrowsForUnknownId() {
        Sessions sessions = new MemorySessions(null, null, 50);
        assertThatThrownBy(() -> sessions.view("nope"))
            .isInstanceOf(Exception.class);
    }
}
