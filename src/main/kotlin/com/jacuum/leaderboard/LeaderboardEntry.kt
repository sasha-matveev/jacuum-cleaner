package com.jacuum.leaderboard

data class LeaderboardEntry(
    val id: String,
    val username: String,
    val avatar: String,
    val mapHash: String,
    val mapSize: String,
    val algoName: String,
    val iterationsUsed: Int,
    val iterationsAvailable: Int,
    val score: Int,
    val completedAt: String,
    val trace: List<TraceEvent>
)
