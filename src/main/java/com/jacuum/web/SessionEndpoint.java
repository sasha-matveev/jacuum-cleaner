package com.jacuum.web;

import com.jacuum.algo.Direction;
import com.jacuum.engine.Sessions;
import com.jacuum.engine.SessionView;
import com.jacuum.map.GameMap;
import com.jacuum.map.Maps;
import com.jacuum.map.SizePreset;
import com.jacuum.web.dto.CreateSessionRequest;
import com.jacuum.web.dto.MapSnapshot;
import com.jacuum.web.dto.SessionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/session")
public final class SessionEndpoint {

    private final Sessions sessions;
    private final Maps maps;
    private final int defaultIterations;

    public SessionEndpoint(final Sessions sessions, final Maps maps,
                           @Value("${game.default-iterations:500}") final int defaultIterations) {
        this.sessions = sessions;
        this.maps = maps;
        this.defaultIterations = defaultIterations;
    }

    @PostMapping
    public SessionResponse create(@RequestBody final CreateSessionRequest req) throws Exception {
        // C4: Validate required fields
        if (req.algoName() == null || req.algoName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "algoName is required");
        }
        if (req.username() == null || req.username().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        if (req.avatar() == null || req.avatar().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "avatar is required");
        }

        // I11: Use Optional to eliminate explicit null check
        final String hash = Optional.ofNullable(req.hash())
            .filter(s -> !s.isBlank())
            .orElseGet(() -> UUID.randomUUID().toString());
        final SizePreset size = sizeFrom(req.size());
        final int iters = req.iterations() > 0 ? req.iterations() : this.defaultIterations;
        final GameMap map = this.maps.generate(hash, size);
        final String id = this.sessions.open(map, req.algoName(), req.username(), req.avatar(), iters);
        return toResponse(id, map, iters);
    }

    @PostMapping("/{id}/start")
    public SessionView start(@PathVariable final String id) throws Exception {
        this.sessions.start(id);
        return this.sessions.view(id);
    }

    @PostMapping("/{id}/pause")
    public SessionView pause(@PathVariable final String id) throws Exception {
        this.sessions.pause(id);
        return this.sessions.view(id);
    }

    @PostMapping("/{id}/resume")
    public SessionView resume(@PathVariable final String id) throws Exception {
        this.sessions.resume(id);
        return this.sessions.view(id);
    }

    @PostMapping("/{id}/stop")
    public SessionView stop(@PathVariable final String id) throws Exception {
        this.sessions.stop(id);
        return this.sessions.view(id);
    }

    @GetMapping("/{id}")
    public SessionView view(@PathVariable final String id) throws Exception {
        return this.sessions.view(id);
    }

    private SessionResponse toResponse(final String id, final GameMap map, final int iters)
            throws Exception {
        final List<MapSnapshot.TileSnapshot> tiles = new ArrayList<>();
        for (int y = 0; y < map.height(); y++) {
            for (int x = 0; x < map.width(); x++) {
                if (map.isFloor(x, y)) {
                    tiles.add(new MapSnapshot.TileSnapshot(x, y,
                        map.hasWall(x, y, Direction.NORTH),
                        map.hasWall(x, y, Direction.SOUTH),
                        map.hasWall(x, y, Direction.EAST),
                        map.hasWall(x, y, Direction.WEST)));
                }
            }
        }
        final MapSnapshot snap = new MapSnapshot(map.width(), map.height(),
            map.startX(), map.startY(), map.totalFloorTiles(), tiles);
        return new SessionResponse(id, "SETUP", snap,
            map.startX(), map.startY(), map.totalFloorTiles(), iters);
    }

    /**
     * Converts a size string to SizePreset. Unknown or blank sizes fall back to SMALL by design.
     * M11: This fallback behavior is intentional for API robustness; no HTTP 400 is thrown for invalid sizes.
     */
    private SizePreset sizeFrom(final String s) {
        if (s == null || s.isBlank()) return SizePreset.SMALL;
        try {
            return SizePreset.valueOf(s.toUpperCase());
        } catch (final IllegalArgumentException e) {
            return SizePreset.SMALL;
        }
    }
}
