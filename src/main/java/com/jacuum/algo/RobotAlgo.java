package com.jacuum.algo;

/**
 * Implement this interface and annotate your class with {@link RobotAlgorithm}
 * to register it as a selectable cleaning algorithm.
 *
 * <p>The engine calls {@link #next(Tile)} once per iteration.
 * Return the {@link Direction} the robot should attempt to move.
 * If a wall blocks the move, the robot stays in place (iteration is still consumed).
 *
 * <p>Throwing any exception is treated as an immediate unsuccessful finish (score = 0).
 * Implementations may be stateful — the engine creates one fresh instance per session.
 */
public interface RobotAlgo {
    Direction next(Tile tile) throws Exception;
}
