package dev.ytype.jacuum.domain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record RoomMap(int width, int height, Coordinate start, Set<Coordinate> floorTiles) {

    public RoomMap {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(floorTiles, "floorTiles");
        validateFloorTiles(width, height, floorTiles);
        floorTiles = Collections.unmodifiableSet(new LinkedHashSet<>(floorTiles));
        if (!floorTiles.contains(start)) {
            throw new IllegalArgumentException("start must be a floor tile");
        }
    }

    public boolean isFloor(Coordinate coordinate) {
        return coordinate != null && floorTiles.contains(coordinate);
    }

    public boolean isWall(Coordinate coordinate) {
        return !isFloor(coordinate);
    }

    public boolean hasWall(Coordinate coordinate, Direction direction) {
        Objects.requireNonNull(coordinate, "coordinate");
        Objects.requireNonNull(direction, "direction");
        return isWall(coordinate.move(direction));
    }

    public int reachableFloorCount() {
        return floorTiles.size();
    }

    private static void validateFloorTiles(int width, int height, Set<Coordinate> floorTiles) {
        for (Coordinate tile : floorTiles) {
            Objects.requireNonNull(tile, "floorTiles contains null");
            if (tile.x() < 0 || tile.x() >= width || tile.y() < 0 || tile.y() >= height) {
                throw new IllegalArgumentException("floor tiles must be within bounds");
            }
        }
    }
}