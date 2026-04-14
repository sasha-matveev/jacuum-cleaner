package dev.ytype.jacuum.web;

import dev.ytype.jacuum.domain.Coordinate;
import dev.ytype.jacuum.domain.RoomMap;
import dev.ytype.jacuum.domain.SizePreset;
import dev.ytype.jacuum.mapgen.GeneratedMap;
import dev.ytype.jacuum.mapgen.MapGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/maps")
public class MapController {

    private final MapGenerator mapGenerator = new MapGenerator();

    @PostMapping
    public MapResponse createMap(@RequestBody(required = false) CreateMapRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }

        SizePreset preset = parsePreset(request.size());
        GeneratedMap generatedMap = mapGenerator.generate(request.hash(), preset);
        RoomMap roomMap = generatedMap.map();

        return new MapResponse(
                generatedMap.hash(),
                preset.name().toLowerCase(Locale.ROOT),
                roomMap.width(),
                roomMap.height(),
                new MapResponse.StartCoordinate(roomMap.start().x(), roomMap.start().y()),
                tileMatrix(roomMap));
    }

    private static SizePreset parsePreset(String size) {
        if (size == null || size.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size is required");
        }

        try {
            return SizePreset.valueOf(size.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown size preset", exception);
        }
    }

    private static List<List<String>> tileMatrix(RoomMap roomMap) {
        List<List<String>> rows = new ArrayList<>(roomMap.height());
        for (int y = 0; y < roomMap.height(); y++) {
            List<String> row = new ArrayList<>(roomMap.width());
            for (int x = 0; x < roomMap.width(); x++) {
                Coordinate coordinate = new Coordinate(x, y);
                row.add(roomMap.isFloor(coordinate) ? "floor" : "wall");
            }
            rows.add(List.copyOf(row));
        }
        return List.copyOf(rows);
    }
}
