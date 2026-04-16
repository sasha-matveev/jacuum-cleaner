package com.jacuum.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class RoomMapGeneratorTest {

    private RoomMapGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new RoomMapGenerator();
    }

    @Test
    void sameHashProducesSameMap() {
        GameMap m1 = generator.generate("abc123", SizePreset.SMALL);
        GameMap m2 = generator.generate("abc123", SizePreset.SMALL);

        boolean[][] w1 = m1.getWallsCopy();
        boolean[][] w2 = m2.getWallsCopy();
        for (int y = 0; y < m1.getHeight(); y++)
            for (int x = 0; x < m1.getWidth(); x++)
                assertThat(w1[y][x]).isEqualTo(w2[y][x]);
    }

    @Test
    void differentHashProducesDifferentMap() {
        GameMap m1 = generator.generate("hash_A", SizePreset.MEDIUM);
        GameMap m2 = generator.generate("hash_B", SizePreset.MEDIUM);

        boolean[][] w1 = m1.getWallsCopy();
        boolean[][] w2 = m2.getWallsCopy();
        boolean anyDiff = false;
        for (int y = 0; y < m1.getHeight(); y++)
            for (int x = 0; x < m1.getWidth(); x++)
                if (w1[y][x] != w2[y][x]) { anyDiff = true; break; }
        assertThat(anyDiff).isTrue();
    }

    @ParameterizedTest
    @EnumSource(SizePreset.class)
    void allFloorTilesReachable(SizePreset preset) {
        GameMap map = generator.generate("reachability-test", preset);
        assertAllFloorTilesReachable(map);
    }

    @ParameterizedTest
    @EnumSource(SizePreset.class)
    void startPositionIsFloorTile(SizePreset preset) {
        GameMap map = generator.generate("start-pos-test", preset);
        assertThat(map.isWall(map.getStartX(), map.getStartY())).isFalse();
    }

    @ParameterizedTest
    @EnumSource(SizePreset.class)
    void mapHasAtLeastSomeFloorTiles(SizePreset preset) {
        GameMap map = generator.generate("floor-count-test", preset);
        assertThat(map.countFloorTiles()).isGreaterThan(4);
    }

    @ParameterizedTest
    @EnumSource(SizePreset.class)
    void outerBorderIsAlwaysWall(SizePreset preset) {
        GameMap map = generator.generate("border-test", preset);
        int w = map.getWidth(), h = map.getHeight();
        for (int x = 0; x < w; x++) {
            assertThat(map.isWall(x, 0)).as("top border x=%d", x).isTrue();
            assertThat(map.isWall(x, h - 1)).as("bottom border x=%d", x).isTrue();
        }
        for (int y = 0; y < h; y++) {
            assertThat(map.isWall(0, y)).as("left border y=%d", y).isTrue();
            assertThat(map.isWall(w - 1, y)).as("right border y=%d", y).isTrue();
        }
    }

    // --- helpers ---

    private void assertAllFloorTilesReachable(GameMap map) {
        int w = map.getWidth(), h = map.getHeight();
        boolean[][] visited = new boolean[h][w];
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{map.getStartX(), map.getStartY()});
        visited[map.getStartY()][map.getStartX()] = true;

        int[][] deltas = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int[] d : deltas) {
                int nx = cur[0] + d[0], ny = cur[1] + d[1];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h
                        && !map.isWall(nx, ny) && !visited[ny][nx]) {
                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (!map.isWall(x, y))
                    assertThat(visited[y][x])
                        .as("Floor tile (%d,%d) should be reachable", x, y)
                        .isTrue();
    }
}
