package com.jacuum.leaderboard

class SilentLeaderboard : Leaderboard {
    override fun entries(): List<LeaderboardEntry> = emptyList()
    override fun save(entry: LeaderboardEntry) {}
}
