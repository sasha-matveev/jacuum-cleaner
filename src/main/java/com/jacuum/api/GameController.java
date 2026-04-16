package com.jacuum.api;

import com.jacuum.game.GameEngine;
import com.jacuum.game.GameSession;
import com.jacuum.game.SseGameService;
import com.jacuum.game.StepResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameEngine engine;
    private final SseGameService sseService;

    public GameController(GameEngine engine, SseGameService sseService) {
        this.engine = engine;
        this.sseService = sseService;
    }

    @PostMapping("/start")
    public SessionInfo start(@RequestBody StartRequest req) {
        GameSession session = engine.startSession(
                req.hash(), req.size(), req.algoId(),
                req.username(), req.avatar(), req.maxIterations());
        return SessionInfo.from(session);
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String id) {
        return sseService.stream(id);
    }

    @PostMapping("/{id}/control")
    public void control(@PathVariable String id, @RequestBody ControlRequest req) {
        switch (req.action().toUpperCase()) {
            case "PAUSE"  -> engine.pauseSession(id);
            case "RESUME" -> engine.resumeSession(id);
            case "ABORT"  -> engine.abortSession(id);
            case "SPEED"  -> sseService.setSpeed(id, req.level() != null ? req.level() : 3);
            default       -> throw new IllegalArgumentException("Unknown action: " + req.action());
        }
    }

    @GetMapping("/{id}/state")
    public StepResult state(@PathVariable String id) {
        GameSession s = engine.getSession(id);
        return new StepResult(s.getRobotX(), s.getRobotY(), false,
                s.getMap().countCleanedTiles(), s.getMap().countFloorTiles(),
                s.getIterationsUsed(), s.getMaxIterations(),
                s.getScore(), s.getStatus());
    }

    // --- DTOs ---

    public record StartRequest(String hash, String size, String algoId,
                                String username, String avatar, int maxIterations) {}

    public record ControlRequest(String action, Integer level) {}

    public record SessionInfo(String sessionId, String hash, String size,
                               int width, int height, int startX, int startY,
                               boolean[][] walls, int floorTiles,
                               String username, String avatar, String algoId,
                               int maxIterations) {
        static SessionInfo from(GameSession s) {
            return new SessionInfo(
                s.getSessionId(),
                s.getMap().getHash(),
                s.getMap().getSizePreset().name().toLowerCase(),
                s.getMap().getWidth(), s.getMap().getHeight(),
                s.getMap().getStartX(), s.getMap().getStartY(),
                s.getMap().getWallsCopy(),
                s.getMap().countFloorTiles(),
                s.getUsername(), s.getAvatar(), s.getAlgoId(),
                s.getMaxIterations()
            );
        }
    }
}
