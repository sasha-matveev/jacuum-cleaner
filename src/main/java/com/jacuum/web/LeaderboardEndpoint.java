package com.jacuum.web;

import com.jacuum.leaderboard.Leaderboard;
import com.jacuum.leaderboard.LeaderboardEntry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public final class LeaderboardEndpoint {

    private final Leaderboard leaderboard;

    public LeaderboardEndpoint(final Leaderboard leaderboard) {
        this.leaderboard = leaderboard;
    }

    @GetMapping
    public List<LeaderboardEntry> entries() throws Exception {
        return leaderboard.entries();
    }

    @PostMapping
    public LeaderboardEntry save(@RequestBody final LeaderboardEntry entry) throws Exception {
        leaderboard.save(entry);
        return entry;
    }
}
