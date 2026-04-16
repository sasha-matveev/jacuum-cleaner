package com.jacuum.api;

import com.jacuum.map.GameMap;
import com.jacuum.map.MapGenerator;
import com.jacuum.map.SizePreset;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MapGenerator mapGenerator;

    public MapController(MapGenerator mapGenerator) {
        this.mapGenerator = mapGenerator;
    }

    @PostMapping("/generate")
    public MapResponse generate(@RequestBody MapRequest req) {
        String hash = (req.hash() == null || req.hash().isBlank())
                ? UUID.randomUUID().toString()
                : req.hash();
        SizePreset size = SizePreset.fromString(req.size());
        GameMap map = mapGenerator.generate(hash, size);
        return MapResponse.from(map, hash);
    }

    // --- DTOs ---

    public record MapRequest(String hash, String size) {}

    public record MapResponse(String hash, String size, int width, int height,
                               int startX, int startY, boolean[][] walls) {
        static MapResponse from(GameMap map, String hash) {
            return new MapResponse(hash, map.getSizePreset().name().toLowerCase(),
                    map.getWidth(), map.getHeight(),
                    map.getStartX(), map.getStartY(),
                    map.getWallsCopy());
        }
    }
}
