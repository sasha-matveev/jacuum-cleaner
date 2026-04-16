package com.jacuum.algo;

import com.jacuum.algo.impl.AlwaysLeftAlgo;
import com.jacuum.algo.impl.RandomAlgo;
import com.jacuum.algo.impl.SmartExplorerAlgo;
import com.jacuum.map.Direction;
import com.jacuum.map.GameMap;
import com.jacuum.map.Tile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Contract tests: every {@link RobotAlgo} implementation must pass these.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>No exception thrown during N iterations.</li>
 *   <li>Robot cleans at least 1 tile.</li>
 *   <li>Algorithm returns non-null Direction.</li>
 * </ul>
 */
class AlgoContractTest {

    private static final int MAX_ITERATIONS = 500;

    static Stream<RobotAlgo> algos() {
        return Stream.of(new RandomAlgo(), new AlwaysLeftAlgo(), new SmartExplorerAlgo());
    }

    static Stream<GameMap> maps() {
        return Stream.of(
            MapFixtures.square5x5(),
            MapFixtures.corridor1x10(),
            MapFixtures.lShape(),
            MapFixtures.roomWithPillar()
        );
    }

    @ParameterizedTest(name = "algo={0} map=square5x5")
    @MethodSource("algos")
    void noExceptionOnSquare(RobotAlgo algo) {
        runAndAssert(algo, MapFixtures.square5x5());
    }

    @ParameterizedTest(name = "algo={0} map=corridor")
    @MethodSource("algos")
    void noExceptionOnCorridor(RobotAlgo algo) {
        runAndAssert(algo, MapFixtures.corridor1x10());
    }

    @ParameterizedTest(name = "algo={0} map=lShape")
    @MethodSource("algos")
    void noExceptionOnLShape(RobotAlgo algo) {
        runAndAssert(algo, MapFixtures.lShape());
    }

    @ParameterizedTest(name = "algo={0} map=roomWithPillar")
    @MethodSource("algos")
    void noExceptionOnRoomWithPillar(RobotAlgo algo) {
        runAndAssert(algo, MapFixtures.roomWithPillar());
    }

    private void runAndAssert(RobotAlgo algo, GameMap map) {
        int[] pos = {map.getStartX(), map.getStartY()};
        map.markCleaned(pos[0], pos[1]);

        assertThatCode(() -> {
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                Tile tile = map.tileViewAt(pos[0], pos[1]);
                Direction dir = algo.next(tile);
                assertThat(dir).as("next() must not return null").isNotNull();

                int nx = pos[0] + dir.dx;
                int ny = pos[1] + dir.dy;
                if (!map.isWall(nx, ny)) {
                    pos[0] = nx;
                    pos[1] = ny;
                    map.markCleaned(pos[0], pos[1]);
                }
            }
        }).doesNotThrowAnyException();

        assertThat(map.countCleanedTiles())
            .as("Robot should clean at least 1 tile")
            .isGreaterThanOrEqualTo(1);
    }
}
