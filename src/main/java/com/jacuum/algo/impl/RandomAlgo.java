package com.jacuum.algo.impl;

import com.jacuum.algo.*;
import java.util.*;

@RobotAlgorithm("Random")
public final class RandomAlgo implements RobotAlgo {
    private final Random rng = new Random();

    @Override
    public Direction next(Tile tile) {
        List<Direction> passable = new ArrayList<>(4);
        for (Direction d : Direction.values())
            if (!tile.hasWall(d)) passable.add(d);
        if (passable.isEmpty())
            return Direction.values()[rng.nextInt(Direction.values().length)];
        return passable.get(rng.nextInt(passable.size()));
    }
}
