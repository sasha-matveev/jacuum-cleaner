package com.jacuum.algo.impl;

import com.jacuum.algo.*;
import java.util.*;

/**
 * Picks uniformly at random from all passable directions (neighbors without walls).
 * When surrounded, picks from all four directions (robot stays in place).
 */
@RobotAlgorithm("Random")
public final class RandomAlgo implements RobotAlgo {
    private final Random rng;

    public RandomAlgo(final Random rng) {
        this.rng = rng;
    }

    @Override
    public Direction next(final Tile tile) {
        final List<Direction> passable = new ArrayList<>(4);
        for (final Direction d : Direction.values())
            if (!tile.hasWall(d)) passable.add(d);
        if (passable.isEmpty())
            return Direction.values()[rng.nextInt(Direction.values().length)];
        return passable.get(rng.nextInt(passable.size()));
    }
}
