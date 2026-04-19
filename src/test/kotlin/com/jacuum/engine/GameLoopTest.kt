package com.jacuum.engine

import com.jacuum.algo.Algorithms
import com.jacuum.algo.Direction
import com.jacuum.algo.RobotAlgo
import com.jacuum.map.GeneratedMap
import com.jacuum.map.SizePreset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameLoopTest {

    private fun eastAlways(): Algorithms = object : Algorithms {
        override fun names() = listOf("east")
        override fun instantiate(name: String): RobotAlgo = RobotAlgo { Direction.EAST }
    }

    private fun corridor(): com.jacuum.map.GameMap {
        val f = Array(3) { BooleanArray(7) }
        for (x in 1 until 6) f[1][x] = true
        return GeneratedMap("corridor", SizePreset.TINY, f, 1, 1)
    }

    @Test fun robotCleansCorridorMovingEast() {
        val sessions = MemorySessions(SilentMessaging(), eastAlways(), 50)
        val id = sessions.open(corridor(), "east", "Bot", "\uD83E\uDD16", 20)
        sessions.start(id)

        val deadline = System.currentTimeMillis() + 2000
        while (sessions.view(id).status != RunStatus.FINISHED
            && System.currentTimeMillis() < deadline)
            Thread.sleep(50)

        val view = sessions.view(id)
        assertThat(view.status).isEqualTo(RunStatus.FINISHED)
        assertThat(view.totalCleaned).isGreaterThan(0)
        assertThat(view.score).isGreaterThan(0)
    }

    @Test fun algoCrashFinishesWithZeroScore() {
        val crashAlgo = object : Algorithms {
            override fun names() = listOf("crash")
            override fun instantiate(name: String): RobotAlgo =
                RobotAlgo { throw RuntimeException("boom") }
        }
        val sessions = MemorySessions(SilentMessaging(), crashAlgo, 50)
        val id = sessions.open(corridor(), "crash", "Bot", "\uD83E\uDD16", 20)
        sessions.start(id)
        val deadline = System.currentTimeMillis() + 2000
        while (sessions.view(id).status != RunStatus.FINISHED
            && System.currentTimeMillis() < deadline)
            Thread.sleep(50)
        assertThat(sessions.view(id).finishReason).isEqualTo(FinishReason.ALGO_CRASH)
    }
}
