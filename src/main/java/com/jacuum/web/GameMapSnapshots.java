package com.jacuum.web;

import com.jacuum.algo.Direction;
import com.jacuum.map.GameMap;
import com.jacuum.web.dto.MapSnapshot;
import java.util.ArrayList;
import java.util.List;

final class GameMapSnapshots implements Snapshots {
    @Override
    public MapSnapshot of(final GameMap map) {
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
        return new MapSnapshot(map.width(), map.height(),
            map.startX(), map.startY(), map.totalFloorTiles(), tiles);
    }
}
