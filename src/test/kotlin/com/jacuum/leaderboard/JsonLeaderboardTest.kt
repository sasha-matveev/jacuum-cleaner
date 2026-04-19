package com.jacuum.leaderboard

import com.jacuum.algo.Direction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Instant

class JsonLeaderboardTest {

    @TempDir
    lateinit var tmp: Path

    private fun entry() = LeaderboardEntry(
        "id1", "Luke", "\uD83E\uDD16", "hash1", "SMALL", "Random",
        42, 100, 4200, Instant.now().toString(),
        listOf(TraceEvent(1, Direction.EAST, 1, 1, 100))
    )

    @Test fun saveAndRetrieveEntry() {
        val file = tmp.resolve("lb.json")
        val lb: Leaderboard = JsonLeaderboard(file)
        lb.save(entry())
        val entries = lb.entries()
        assertThat(entries).hasSize(1)
        assertThat(entries[0].username).isEqualTo("Luke")
    }

    @Test fun persistsAcrossInstances() {
        val file = tmp.resolve("lb2.json")
        JsonLeaderboard(file).save(entry())
        val entries = JsonLeaderboard(file).entries()
        assertThat(entries).hasSize(1)
    }

    @Test fun silentLeaderboardDoesNothing() {
        val lb: Leaderboard = SilentLeaderboard()
        lb.save(entry())
        assertThat(lb.entries()).isEmpty()
    }
}
