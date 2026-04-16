package com.jacuum.algo.impl;

import com.jacuum.algo.RobotAlgo;
import com.jacuum.algo.VacuumAlgo;
import com.jacuum.map.Direction;
import com.jacuum.map.Tile;

import java.util.Random;

/**
 * Picks a random direction each iteration. Simple baseline algorithm.
 */
@VacuumAlgo("Random")
public class RandomAlgo implements RobotAlgo {

    private final Random rng = new Random();
    private final Direction[] directions = Direction.values();

    @Override
    public Direction next(Tile currentTile) {
        return directions[rng.nextInt(directions.length)];
    }
}
