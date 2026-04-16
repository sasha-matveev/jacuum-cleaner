package com.jacuum.leaderboard;

import com.jacuum.algo.impl.RandomAlgo;
import com.jacuum.map.GameMap;
import com.jacuum.map.RoomMapGenerator;
import com.jacuum.map.SizePreset;
import com.jacuum.game.GameSession;
import com.jacuum.game.GameStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class LeaderboardServiceTest {

    @Autowired
    LeaderboardService service;

    @Test
    void saveAndRetrieveEntry() {
        GameMap map = new RoomMapGenerator().generate("lb-test", SizePreset.TINY);
        GameSession session = new GameSession(
            "test-session-id", map, new RandomAlgo(), "Random",
            "HeroUser", "🤖", 100);
        session.addTrace("UP");
        session.addTrace("LEFT");
        session.setStatus(GameStatus.FINISHED);

        LeaderboardEntry saved = service.save(session);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("HeroUser");
        assertThat(saved.getScore()).isGreaterThanOrEqualTo(0);
        assertThat(saved.getTrace()).isEqualTo("UP,LEFT");

        List<LeaderboardEntry> all = service.getAll();
        assertThat(all).anyMatch(e -> e.getId().equals(saved.getId()));
    }

    @Test
    void findByIdReturnsPresentForExisting() {
        GameMap map = new RoomMapGenerator().generate("lb-find-test", SizePreset.TINY);
        GameSession session = new GameSession(
            "find-session-id", map, new RandomAlgo(), "Random",
            "FindMe", "🦾", 200);
        session.setStatus(GameStatus.FINISHED);

        LeaderboardEntry saved = service.save(session);
        assertThat(service.findById(saved.getId())).isPresent();
        assertThat(service.findById("nonexistent")).isEmpty();
    }
}
