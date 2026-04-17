package com.jacuum.web;

import com.jacuum.leaderboard.LeaderboardEntry;
import java.util.List;

interface LeaderboardApi {
    List<LeaderboardEntry> entries() throws Exception;
    LeaderboardEntry save(LeaderboardEntry entry) throws Exception;
}
