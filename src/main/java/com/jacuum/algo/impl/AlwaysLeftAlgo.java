package com.jacuum.algo.impl;

import com.jacuum.algo.*;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

/**
 * Prefers WEST (left), then falls back clockwise: WEST → NORTH → EAST → SOUTH.
 * When surrounded by walls, returns WEST (robot stays in place for that iteration).
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RobotAlgorithm("Always Left")
public final class AlwaysLeftAlgo implements RobotAlgo {

    private final java.util.List<Direction> preference;

    public AlwaysLeftAlgo() {
        this.preference = java.util.List.of(Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH);
    }

    @Override
    public Direction next(final Tile tile) {
        for (final Direction d : this.preference) {
            if (!tile.hasWall(d)) return d;
        }
        return Direction.WEST; // surrounded — stay put next iteration
    }
}
