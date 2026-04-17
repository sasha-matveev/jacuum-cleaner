package com.jacuum.leaderboard;

import java.util.List;

public final class SilentLeaderboard implements Leaderboard {
    @Override public List<LeaderboardEntry> entries() { return List.of(); }
    @Override public void save(final LeaderboardEntry entry) { /* no-op */ }
}
