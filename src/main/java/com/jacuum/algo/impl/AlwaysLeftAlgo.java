package com.jacuum.algo.impl;

import com.jacuum.algo.RobotAlgo;
import com.jacuum.algo.VacuumAlgo;
import com.jacuum.map.Direction;
import com.jacuum.map.Tile;

/**
 * Always tries LEFT first. If blocked, tries DOWN → RIGHT → UP in order.
 * A deterministic "wall-follower" baseline.
 */
@VacuumAlgo("Always Left")
public class AlwaysLeftAlgo implements RobotAlgo {

    private static final Direction[] PRIORITY = {
        Direction.LEFT, Direction.DOWN, Direction.RIGHT, Direction.UP
    };

    @Override
    public Direction next(Tile currentTile) {
        for (Direction d : PRIORITY) {
            if (!currentTile.hasWall(d)) return d;
        }
        // Fully enclosed — just return LEFT (will be a no-op)
        return Direction.LEFT;
    }
}
