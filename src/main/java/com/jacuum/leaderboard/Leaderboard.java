package com.jacuum.leaderboard;

import java.util.List;

public interface Leaderboard {
    List<LeaderboardEntry> entries() throws Exception;
    void save(LeaderboardEntry entry) throws Exception;
}
