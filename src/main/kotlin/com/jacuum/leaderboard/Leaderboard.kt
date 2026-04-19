package com.jacuum.leaderboard

interface Leaderboard {
    @Throws(Exception::class)
    fun entries(): List<LeaderboardEntry>
    @Throws(Exception::class)
    fun save(entry: LeaderboardEntry)
}
