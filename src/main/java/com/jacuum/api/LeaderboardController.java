package com.jacuum.api;

import com.jacuum.game.GameEngine;
import com.jacuum.game.GameSession;
import com.jacuum.leaderboard.LeaderboardEntry;
import com.jacuum.leaderboard.LeaderboardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService service;
    private final GameEngine engine;

    @Value("${leaderboard.path:}")
    private String leaderboardPath;

    public LeaderboardController(LeaderboardService service, GameEngine engine) {
        this.service = service;
        this.engine = engine;
    }

    @GetMapping("/status")
    public StatusResponse status() {
        boolean persistent = leaderboardPath != null && !leaderboardPath.isBlank();
        return new StatusResponse(persistent, persistent ? leaderboardPath : null);
    }

    @GetMapping
    public List<LeaderboardEntry> list() {
        return service.getAll();
    }

    @PostMapping
    public LeaderboardEntry save(@RequestBody SaveRequest req) {
        GameSession session = engine.getSession(req.sessionId());
        return service.save(session);
    }

    @GetMapping("/{id}/replay")
    public ResponseEntity<ReplayResponse> replay(@PathVariable String id) {
        return service.findById(id)
                .map(e -> ResponseEntity.ok(new ReplayResponse(
                        e.getMapHash(), e.getMapSize(),
                        e.getMaxIterations(),
                        Arrays.asList(e.getTrace().split(",")))))
                .orElse(ResponseEntity.notFound().build());
    }

    // --- DTOs ---

    public record StatusResponse(boolean persistent, String path) {}
    public record SaveRequest(String sessionId) {}

    public record ReplayResponse(String hash, String size, int maxIterations,
                                  List<String> trace) {}
}
