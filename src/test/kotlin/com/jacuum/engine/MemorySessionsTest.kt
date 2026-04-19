package com.jacuum.engine

import com.jacuum.map.GeneratedMap
import com.jacuum.map.SizePreset
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class MemorySessionsTest {

    private fun smallMap(): com.jacuum.map.GameMap {
        val f = Array(3) { BooleanArray(3) { true } }
        return GeneratedMap("test", SizePreset.TINY, f, 1, 1)
    }

    @Test fun openCreatesSessionInSetupState() {
        val sessions = MemorySessions(SilentMessaging(), null, 50)
        val id = sessions.open(smallMap(), "RandomAlgo", "Alice", "\uD83E\uDD16", 100)
        assertThat(id).isNotBlank()
        val view = sessions.view(id)
        assertThat(view.status).isEqualTo(RunStatus.SETUP)
        assertThat(view.robotX).isEqualTo(1)
        assertThat(view.robotY).isEqualTo(1)
        assertThat(view.score).isEqualTo(0)
        assertThat(view.totalFloor).isEqualTo(9)
    }

    @Test fun viewThrowsForUnknownId() {
        val sessions = MemorySessions(SilentMessaging(), null, 50)
        assertThatThrownBy { sessions.view("nope") }.isInstanceOf(Exception::class.java)
    }

    @Test fun sessionCapRejected() {
        val sessions = MemorySessions(SilentMessaging(), null, 2)
        sessions.open(smallMap(), "a", "Alice", "\uD83E\uDD16", 100)
        sessions.open(smallMap(), "a", "Bob", "\uD83E\uDD16", 100)
        assertThatThrownBy { sessions.open(smallMap(), "a", "Charlie", "\uD83E\uDD16", 100) }
            .isInstanceOf(Exception::class.java)
            .hasMessageContaining("Session cap reached")
    }

    @Test fun pauseRequiresRunningState() {
        val sessions = MemorySessions(SilentMessaging(), null, 50)
        val id = sessions.open(smallMap(), "a", "Alice", "\uD83E\uDD16", 100)
        assertThatThrownBy { sessions.pause(id) }.isInstanceOf(Exception::class.java)
    }

    @Test fun resumeRequiresPausedState() {
        val sessions = MemorySessions(SilentMessaging(), null, 50)
        val id = sessions.open(smallMap(), "a", "Alice", "\uD83E\uDD16", 100)
        assertThatThrownBy { sessions.resume(id) }.isInstanceOf(Exception::class.java)
    }
}
