package dev.ytype.jacuum.algo;

import dev.ytype.jacuum.domain.Direction;
import dev.ytype.jacuum.domain.TileView;

/**
 * Produces the next virtual move for a robot.
 * Implementations may keep per-run state. The engine creates a fresh instance per run.
 */
public interface RobotAlgorithm {

    Direction next(TileView tile);
}
