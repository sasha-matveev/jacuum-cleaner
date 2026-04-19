package com.jacuum.web

import com.jacuum.algo.Direction
import com.jacuum.map.GameMap
import com.jacuum.web.dto.MapSnapshot

internal class GameMapSnapshots : Snapshots {
    override fun of(map: GameMap): MapSnapshot {
        val tiles = mutableListOf<MapSnapshot.TileSnapshot>()
        for (y in 0 until map.height()) for (x in 0 until map.width()) {
            if (map.isFloor(x, y)) tiles.add(
                MapSnapshot.TileSnapshot(
                    x, y,
                    map.hasWall(x, y, Direction.NORTH),
                    map.hasWall(x, y, Direction.SOUTH),
                    map.hasWall(x, y, Direction.EAST),
                    map.hasWall(x, y, Direction.WEST)
                )
            )
        }
        return MapSnapshot(
            map.width(), map.height(),
            map.startX(), map.startY(), map.totalFloorTiles(), tiles
        )
    }
}
