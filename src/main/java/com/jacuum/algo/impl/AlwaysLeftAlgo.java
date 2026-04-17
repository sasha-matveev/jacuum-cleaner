package com.jacuum.algo.impl;

import com.jacuum.algo.*;

/**
 * Prefers WEST (left), then falls back clockwise: WEST → NORTH → EAST → SOUTH.
 * When surrounded by walls, returns WEST (robot stays in place for that iteration).
 */
@RobotAlgorithm("Always Left")
public final class AlwaysLeftAlgo implements RobotAlgo {

    @Override
    public Direction next(final Tile tile) {
        final Direction[] preference =
            {Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH};
        for (final Direction d : preference) {
            if (!tile.hasWall(d)) return d;
        }
        return Direction.WEST; // surrounded — stay put next iteration
    }
}
