package com.jacuum.algo;

import com.jacuum.algo.impl.SmartExplorerAlgo;
import com.jacuum.map.Direction;
import com.jacuum.map.GameMap;
import com.jacuum.map.Tile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Specific tests for {@link SmartExplorerAlgo} beyond the shared contract suite.
 * Verifies: complete coverage, zero wall bumps, efficiency, and state isolation.
 */
class SmartExplorerAlgoTest {

    // ---- helpers ----

    private record RunResult(int cleanedTiles, int totalFloor, int iterations, int wallBumps) {}

    private RunResult run(SmartExplorerAlgo algo, GameMap map, int maxIterations) {
        int[] pos = {map.getStartX(), map.getStartY()};
        map.markCleaned(pos[0], pos[1]);

        int wallBumps = 0;
        int i = 0;
        for (; i < maxIterations; i++) {
            if (map.countCleanedTiles() == map.countFloorTiles()) break;
            Tile tile = map.tileViewAt(pos[0], pos[1]);
            Direction dir = algo.next(tile);
            int nx = pos[0] + dir.dx, ny = pos[1] + dir.dy;
            if (map.isWall(nx, ny)) {
                wallBumps++;
            } else {
                pos[0] = nx;
                pos[1] = ny;
                map.markCleaned(pos[0], pos[1]);
            }
        }
        return new RunResult(map.countCleanedTiles(), map.countFloorTiles(), i, wallBumps);
    }

    // ---- zero wall bumps ----

    static Stream<GameMap> allMaps() {
        return Stream.of(
            MapFixtures.square5x5(),
            MapFixtures.corridor1x10(),
            MapFixtures.lShape(),
            MapFixtures.roomWithPillar()
        );
    }

    @ParameterizedTest
    @MethodSource("allMaps")
    void neverBumpsWalls(GameMap map) {
        RunResult r = run(new SmartExplorerAlgo(), map, map.countFloorTiles() * 4);
        assertThat(r.wallBumps())
            .as("SmartExplorer must never attempt a wall move")
            .isZero();
    }

    // ---- complete coverage ----

    @ParameterizedTest
    @MethodSource("allMaps")
    void achievesFullCoverage(GameMap map) {
        int floor = map.countFloorTiles();
        RunResult r = run(new SmartExplorerAlgo(), map, floor * 4);
        assertThat(r.cleanedTiles())
            .as("SmartExplorer must clean every reachable tile")
            .isEqualTo(floor);
    }

    // ---- efficiency ----

    @Test
    void coversSquare5x5InUnder2xFloorTiles() {
        // Optimal snake path = 25 moves; BFS overhead ≤ ~25 extra for backtracking → ≤ 50
        GameMap map = MapFixtures.square5x5();
        int floor = map.countFloorTiles(); // 25
        RunResult r = run(new SmartExplorerAlgo(), map, floor * 4);

        assertThat(r.cleanedTiles()).isEqualTo(floor);
        assertThat(r.iterations())
            .as("Should cover %d tiles in under %d iterations", floor, floor * 2)
            .isLessThanOrEqualTo(floor * 2);
    }

    @Test
    void coversCorridor1x10InMinimalIterations() {
        // Corridor is a straight line — zero backtracking needed
        GameMap map = MapFixtures.corridor1x10();
        int floor = map.countFloorTiles(); // 10
        RunResult r = run(new SmartExplorerAlgo(), map, floor * 4);

        assertThat(r.cleanedTiles()).isEqualTo(floor);
        // Start is at one end; should walk straight across — exactly floor-1 moves
        assertThat(r.iterations())
            .as("Corridor coverage should need exactly floor-1 moves")
            .isEqualTo(floor - 1);
    }

    // ---- state isolation between games (singleton-scope guard) ----

    @Test
    void freshInstanceCoversMultipleGamesIndependently() {
        // Simulate two successive games on different maps with the SAME instance.
        // The algo's position-tracking reset logic should handle this transparently.
        SmartExplorerAlgo algo = new SmartExplorerAlgo();

        GameMap map1 = MapFixtures.square5x5();
        RunResult r1 = run(algo, map1, map1.countFloorTiles() * 4);
        assertThat(r1.cleanedTiles()).isEqualTo(r1.totalFloor());

        // Second "game" on a different map — algo must reset internal state
        GameMap map2 = MapFixtures.corridor1x10();
        RunResult r2 = run(algo, map2, map2.countFloorTiles() * 4);
        assertThat(r2.cleanedTiles()).isEqualTo(r2.totalFloor());
    }

    // ---- room with pillar ----

    @Test
    void coversRoomWithPillarFully() {
        GameMap map = MapFixtures.roomWithPillar();
        int floor = map.countFloorTiles();
        RunResult r = run(new SmartExplorerAlgo(), map, floor * 4);

        assertThat(r.cleanedTiles()).isEqualTo(floor);
        assertThat(r.wallBumps()).isZero();
    }
}
