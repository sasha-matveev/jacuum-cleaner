package com.jacuum.algo;

import com.jacuum.algo.impl.AlwaysLeftAlgo;
import com.jacuum.algo.impl.RandomAlgo;
import com.jacuum.map.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Random;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class AlgoSmokeTest {

    static Stream<RobotAlgo> algos() {
        return Stream.of(new RandomAlgo(new Random()), new AlwaysLeftAlgo());
    }

    static GameMap squareMap() {
        boolean[][] f = new boolean[7][7];
        for (int y = 1; y < 6; y++) for (int x = 1; x < 6; x++) f[y][x] = true;
        return new GeneratedMap("sq", SizePreset.TINY, f, 3, 3);
    }

    static GameMap corridorMap() {
        boolean[][] f = new boolean[3][9];
        for (int x = 1; x < 8; x++) f[1][x] = true;
        return new GeneratedMap("corr", SizePreset.TINY, f, 1, 1);
    }

    @ParameterizedTest @MethodSource("algos")
    void doesNotThrowOnSquareMap(RobotAlgo algo) throws Exception {
        runAlgo(algo, squareMap(), 50);
    }

    @ParameterizedTest @MethodSource("algos")
    void doesNotThrowOnCorridor(RobotAlgo algo) throws Exception {
        runAlgo(algo, corridorMap(), 30);
    }

    private void runAlgo(RobotAlgo algo, GameMap map, int iterations) throws Exception {
        int x = map.startX(), y = map.startY();
        var cleaned = new java.util.HashSet<String>();
        for (int i = 0; i < iterations; i++) {
            final int fx = x, fy = y;
            final var fcleaned = cleaned;
            Tile tile = new Tile() {
                @Override public int x() { return fx; }
                @Override public int y() { return fy; }
                @Override public boolean isClean() { return fcleaned.contains(fx + "," + fy); }
                @Override public boolean hasWall(Direction dir) { return map.hasWall(fx, fy, dir); }
            };
            Direction dir = algo.next(tile);
            assertThat(dir).isNotNull();
            if (!map.hasWall(x, y, dir)) {
                x += dir.dx();
                y += dir.dy();
            }
            cleaned.add(x + "," + y);
        }
    }
}
