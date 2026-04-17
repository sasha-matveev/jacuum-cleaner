package com.jacuum.leaderboard;

import java.util.List;

public record LeaderboardEntry(
    String id,
    String username,
    String avatar,
    String mapHash,
    String mapSize,
    String algoName,
    int iterationsUsed,
    int iterationsAvailable,
    int score,
    String completedAt,
    List<TraceEvent> trace
) {}
