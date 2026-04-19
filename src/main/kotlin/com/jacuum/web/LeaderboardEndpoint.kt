package com.jacuum.web

import com.jacuum.leaderboard.Leaderboard
import com.jacuum.leaderboard.LeaderboardEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/leaderboard")
class LeaderboardEndpoint(private val leaderboard: Leaderboard) : LeaderboardApi {
    @GetMapping
    override fun entries(): List<LeaderboardEntry> = leaderboard.entries()

    @PostMapping
    override fun save(@RequestBody entry: LeaderboardEntry): LeaderboardEntry {
        leaderboard.save(entry); return entry
    }
}
