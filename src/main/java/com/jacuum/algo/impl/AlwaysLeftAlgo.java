package com.jacuum.algo.impl;

import com.jacuum.algo.*;

/**
 * Prefers WEST (left), then falls back clockwise: WEST → NORTH → EAST → SOUTH.
 */
@RobotAlgorithm("Always Left")
public final class AlwaysLeftAlgo implements RobotAlgo {
    private final Direction[] preference =
        {Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH};

    @Override
    public Direction next(Tile tile) {
        for (Direction d : preference)
            if (!tile.hasWall(d)) return d;
        return Direction.WEST; // surrounded — stay put next iteration
    }
}
