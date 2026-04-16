package com.jacuum.map;

/**
 * Immutable snapshot of a tile passed to the robot algorithm each iteration.
 */
public record TileView(int x, int y, boolean clean, boolean wallUp, boolean wallDown,
                       boolean wallLeft, boolean wallRight) implements Tile {

    @Override public int getX() { return x; }
    @Override public int getY() { return y; }
    @Override public boolean isClean() { return clean; }

    @Override
    public boolean hasWall(Direction direction) {
        return switch (direction) {
            case UP    -> wallUp;
            case DOWN  -> wallDown;
            case LEFT  -> wallLeft;
            case RIGHT -> wallRight;
        };
    }
}
