package com.jacuum.leaderboard;

import com.jacuum.game.GameSession;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class LeaderboardService {

    private final LeaderboardRepository repo;

    public LeaderboardService(LeaderboardRepository repo) {
        this.repo = repo;
    }

    public List<LeaderboardEntry> getAll() {
        return repo.findAllByOrderByScoreDescPlayedAtDesc();
    }

    public LeaderboardEntry save(GameSession session) {
        LeaderboardEntry entry = LeaderboardEntry.builder()
                .username(session.getUsername())
                .avatar(session.getAvatar())
                .mapHash(session.getMap().getHash())
                .mapSize(session.getMap().getSizePreset().name().toLowerCase())
                .maxIterations(session.getMaxIterations())
                .iterationsUsed(session.getIterationsUsed())
                .score(session.getScore())
                .trace(String.join(",", session.getTrace()))
                .playedAt(Instant.now())
                .build();
        return repo.save(entry);
    }

    public Optional<LeaderboardEntry> findById(String id) {
        return repo.findById(id);
    }
}
