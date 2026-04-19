package com.jacuum.web

import com.jacuum.leaderboard.LeaderboardEntry

internal interface LeaderboardApi {
    @Throws(Exception::class) fun entries(): List<LeaderboardEntry>
    @Throws(Exception::class) fun save(entry: LeaderboardEntry): LeaderboardEntry
}
