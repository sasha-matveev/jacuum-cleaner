package dev.ytype.jacuum.mapgen;

import dev.ytype.jacuum.domain.Coordinate;
import dev.ytype.jacuum.domain.RoomMap;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class MapValidator {

    private MapValidator() {
    }

    public static boolean isValid(RoomMap map) {
        Objects.requireNonNull(map, "map");
        return hasClosedBoundary(map) && isReachableFromStart(map);
    }

    public static boolean hasClosedBoundary(RoomMap map) {
        Objects.requireNonNull(map, "map");
        for (int x = 0; x < map.width(); x++) {
            if (map.isFloor(new Coordinate(x, 0)) || map.isFloor(new Coordinate(x, map.height() - 1))) {
                return false;
            }
        }
        for (int y = 0; y < map.height(); y++) {
            if (map.isFloor(new Coordinate(0, y)) || map.isFloor(new Coordinate(map.width() - 1, y))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isReachableFromStart(RoomMap map) {
        Objects.requireNonNull(map, "map");

        Set<Coordinate> visited = new HashSet<>();
        ArrayDeque<Coordinate> queue = new ArrayDeque<>();
        queue.add(map.start());
        visited.add(map.start());

        while (!queue.isEmpty()) {
            Coordinate current = queue.removeFirst();
            for (Coordinate neighbor : neighbors(current)) {
                if (map.isFloor(neighbor) && visited.add(neighbor)) {
                    queue.addLast(neighbor);
                }
            }
        }

        return visited.size() == map.reachableFloorCount();
    }

    private static Set<Coordinate> neighbors(Coordinate coordinate) {
        Set<Coordinate> neighbors = new HashSet<>(4);
        neighbors.add(new Coordinate(coordinate.x(), coordinate.y() - 1));
        neighbors.add(new Coordinate(coordinate.x() + 1, coordinate.y()));
        neighbors.add(new Coordinate(coordinate.x(), coordinate.y() + 1));
        neighbors.add(new Coordinate(coordinate.x() - 1, coordinate.y()));
        return neighbors;
    }
}
