package com.jacuum.map;

/**
 * Represents a single tile on the game map as seen by the robot algorithm.
 *
 * <p>A tile is a floor cell that the robot can stand on. Wall cells are not
 * exposed as Tile instances — their presence is reflected by {@link #hasWall(Direction)}.
 *
 * <p>Implementations are immutable snapshots: values reflect the map state at
 * the moment the tile was passed to {@link com.jacuum.algo.RobotAlgo#next(Tile)}.
 */
public interface Tile {

    /** Column index (0-based, left = 0). */
    int getX();

    /** Row index (0-based, top = 0). */
    int getY();

    /**
     * Returns {@code true} if this tile has already been cleaned by the robot
     * during the current session.
     */
    boolean isClean();

    /**
     * Returns {@code true} if moving in the given {@code direction} from this tile
     * would be blocked by a wall (the robot would stay in place).
     */
    boolean hasWall(Direction direction);
}
