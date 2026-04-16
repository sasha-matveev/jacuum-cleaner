package com.jacuum.algo;

/**
 * Read-only view of the robot's current position on the map.
 * Passed to {@link RobotAlgo#next(Tile)} on every iteration.
 */
public interface Tile {
    /** Grid column (0-based, left to right). */
    int x();
    /** Grid row (0-based, top to bottom). */
    int y();
    /** True if this tile has already been cleaned in this session. */
    boolean isClean();
    /** True if moving in the given direction from this tile is blocked by a wall. */
    boolean hasWall(Direction direction);
}
