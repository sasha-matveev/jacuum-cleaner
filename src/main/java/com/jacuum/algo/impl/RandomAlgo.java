package com.jacuum.algo.impl;

import com.jacuum.algo.*;
import java.util.*;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

/**
 * Picks uniformly at random from all passable directions (neighbors without walls).
 * When surrounded, picks from all four directions (robot stays in place).
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RobotAlgorithm("Random")
public final class RandomAlgo implements RobotAlgo {
    private final Random rng;

    public RandomAlgo() {
        this.rng = new Random();
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
