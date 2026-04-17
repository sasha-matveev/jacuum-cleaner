package com.jacuum.leaderboard;

import com.jacuum.algo.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class JsonLeaderboardTest {

    @TempDir Path tmp;

    private LeaderboardEntry entry() {
        return new LeaderboardEntry(
            "id1", "Luke", "🤖", "hash1", "SMALL", "Random",
            42, 100, 4200, Instant.now().toString(),
            List.of(new TraceEvent(1, Direction.EAST, 1, 1, 100))
        );
    }

    @Test void saveAndRetrieveEntry() throws Exception {
        Path file = tmp.resolve("lb.json");
        Leaderboard lb = new JsonLeaderboard(file);
        lb.save(entry());
        List<LeaderboardEntry> entries = lb.entries();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).username()).isEqualTo("Luke");
    }

    @Test void persistsAcrossInstances() throws Exception {
        Path file = tmp.resolve("lb2.json");
        new JsonLeaderboard(file).save(entry());
        List<LeaderboardEntry> entries = new JsonLeaderboard(file).entries();
        assertThat(entries).hasSize(1);
    }

    @Test void silentLeaderboardDoesNothing() throws Exception {
        Leaderboard lb = new SilentLeaderboard();
        lb.save(entry());
        assertThat(lb.entries()).isEmpty();
    }
}
