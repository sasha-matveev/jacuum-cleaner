package dev.ytype.jacuum.algo;

import dev.ytype.jacuum.domain.Direction;
import dev.ytype.jacuum.domain.TileView;

import java.util.List;
import java.util.Objects;

@RobotAlgo(
        id = "wall-follower",
        name = "Wall Follower",
        description = "Prefers left, then forward, then right, then back.")
public class WallFollowerAlgorithm implements RobotAlgorithm {

    private Direction facing = Direction.UP;

    @Override
    public Direction next(TileView tile) {
        Objects.requireNonNull(tile, "tile");
        for (Direction direction : directionsInPriorityOrder(facing)) {
            if (!tile.isWall(direction)) {
                facing = direction;
                return direction;
            }
        }
        return facing;
    }

    private static List<Direction> directionsInPriorityOrder(Direction facing) {
        return switch (facing) {
            case UP -> List.of(Direction.LEFT, Direction.UP, Direction.RIGHT, Direction.DOWN);
            case RIGHT -> List.of(Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT);
            case DOWN -> List.of(Direction.RIGHT, Direction.DOWN, Direction.LEFT, Direction.UP);
            case LEFT -> List.of(Direction.DOWN, Direction.LEFT, Direction.UP, Direction.RIGHT);
        };
    }
}
