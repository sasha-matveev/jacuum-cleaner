package dev.ytype.jacuum.domain;

public record TileView(
        Coordinate coordinate,
        boolean clean,
        int iteration,
        boolean wallUp,
        boolean wallRight,
        boolean wallDown,
        boolean wallLeft) {

    public boolean isWall(Direction direction) {
        return switch (direction) {
            case UP -> wallUp;
            case RIGHT -> wallRight;
            case DOWN -> wallDown;
            case LEFT -> wallLeft;
        };
    }
}