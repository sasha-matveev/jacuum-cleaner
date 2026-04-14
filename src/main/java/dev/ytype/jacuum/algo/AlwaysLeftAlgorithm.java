package dev.ytype.jacuum.algo;

import dev.ytype.jacuum.domain.Direction;
import dev.ytype.jacuum.domain.TileView;

@RobotAlgo(
        id = "always-left",
        name = "Always Left",
        description = "Always requests LEFT.")
public class AlwaysLeftAlgorithm implements RobotAlgorithm {

    @Override
    public Direction next(TileView tile) {
        return Direction.LEFT;
    }
}
