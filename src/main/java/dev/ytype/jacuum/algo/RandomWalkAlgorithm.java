package dev.ytype.jacuum.algo;

import dev.ytype.jacuum.domain.Direction;
import dev.ytype.jacuum.domain.TileView;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@RobotAlgo(
        id = "random-walk",
        name = "Random Walk",
        description = "Chooses a random legal direction.")
public class RandomWalkAlgorithm implements RobotAlgorithm {

    @Override
    public Direction next(TileView tile) {
        Objects.requireNonNull(tile, "tile");
        List<Direction> legalDirections = Arrays.stream(Direction.values())
                .filter(direction -> !tile.isWall(direction))
                .toList();
        if (legalDirections.isEmpty()) {
            return Direction.UP;
        }
        return legalDirections.get(ThreadLocalRandom.current().nextInt(legalDirections.size()));
    }
}
