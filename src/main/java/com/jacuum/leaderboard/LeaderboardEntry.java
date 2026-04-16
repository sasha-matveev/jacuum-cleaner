package com.jacuum.leaderboard;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "leaderboard")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String username;
    private String avatar;
    private String mapHash;
    private String mapSize;
    private int maxIterations;
    private int iterationsUsed;
    private int score;

    /** Comma-separated direction names for replay. */
    @Column(length = 100000)
    private String trace;

    private Instant playedAt;
}
