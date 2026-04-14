package dev.ytype.jacuum.engine;

import dev.ytype.jacuum.algo.AlwaysLeftAlgorithm;
import dev.ytype.jacuum.algo.RandomWalkAlgorithm;
import dev.ytype.jacuum.algo.RobotAlgorithm;
import dev.ytype.jacuum.algo.WallFollowerAlgorithm;
import dev.ytype.jacuum.domain.Coordinate;
import dev.ytype.jacuum.domain.RoomMap;
import dev.ytype.jacuum.domain.RunStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class SampleAlgorithmSmokeTest {

    private final RunEngine engine = new RunEngine();

    @Test
    void sampleAlgorithmsRunOnPredefinedMapsWithoutThrowing() {
        List<Supplier<RobotAlgorithm>> algorithms = List.of(
                RandomWalkAlgorithm::new,
                AlwaysLeftAlgorithm::new,
                WallFollowerAlgorithm::new);

        for (Supplier<RobotAlgorithm> algorithmFactory : algorithms) {
            for (RoomMap map : predefinedMaps()) {
                RunSession session = engine.start(new RunRequest(map, algorithmFactory, 12));
                RunResult result = session.step(12);

                assertThat(result.status())
                        .as("algorithm %s on map %s", algorithmFactory, map)
                        .isNotEqualTo(RunStatus.FAILED);
            }
        }
    }

    private static List<RoomMap> predefinedMaps() {
        return List.of(
                squareRoom(),
                corridor(),
                loopRoute(),
                roomWithInternalObstacle(),
                singleTileRoom());
    }

    private static RoomMap squareRoom() {
        return new RoomMap(
                5,
                5,
                new Coordinate(2, 2),
                Set.of(
                        new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(3, 1),
                        new Coordinate(1, 2), new Coordinate(2, 2), new Coordinate(3, 2),
                        new Coordinate(1, 3), new Coordinate(2, 3), new Coordinate(3, 3)));
    }

    private static RoomMap corridor() {
        return new RoomMap(
                5,
                3,
                new Coordinate(1, 1),
                Set.of(
                        new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(3, 1)));
    }

    private static RoomMap loopRoute() {
        return new RoomMap(
                5,
                5,
                new Coordinate(1, 1),
                Set.of(
                        new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(3, 1),
                        new Coordinate(1, 2), new Coordinate(3, 2),
                        new Coordinate(1, 3), new Coordinate(2, 3), new Coordinate(3, 3)));
    }

    private static RoomMap roomWithInternalObstacle() {
        return new RoomMap(
                5,
                5,
                new Coordinate(1, 1),
                Set.of(
                        new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(3, 1),
                        new Coordinate(1, 2), new Coordinate(3, 2),
                        new Coordinate(1, 3), new Coordinate(2, 3), new Coordinate(3, 3)));
    }

    private static RoomMap singleTileRoom() {
        return new RoomMap(1, 1, new Coordinate(0, 0), Set.of(new Coordinate(0, 0)));
    }
}
