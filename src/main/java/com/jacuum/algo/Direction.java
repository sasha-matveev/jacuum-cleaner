package com.jacuum.algo;

/**
 * One of the four cardinal directions a robot can move.
 *
 * <p>Coordinate convention: x increases East, y increases South (screen space).
 * So {@link #SOUTH} has {@code dy = +1} and {@link #NORTH} has {@code dy = -1}.
 */
public enum Direction {
    NORTH, SOUTH, EAST, WEST;

    /**
     * Returns the direction directly opposite to this one.
     * For example, {@code NORTH.opposite()} returns {@code SOUTH}.
     */
    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST  -> WEST;
            case WEST  -> EAST;
        };
    }

    /**
     * Horizontal offset: {@code +1} for EAST, {@code -1} for WEST, {@code 0} otherwise.
     */
    public int dx() {
        return switch (this) {
            case EAST  ->  1;
            case WEST  -> -1;
            default    ->  0;
        };
    }

    /**
     * Vertical offset (screen space): {@code +1} for SOUTH, {@code -1} for NORTH, {@code 0} otherwise.
     */
    public int dy() {
        return switch (this) {
            case SOUTH ->  1;
            case NORTH -> -1;
            default    ->  0;
        };
    }
}
