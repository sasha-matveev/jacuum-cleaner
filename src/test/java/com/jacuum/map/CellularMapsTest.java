package com.jacuum.map;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CellularMapsTest {

    private final Maps maps = new CellularMaps();

    @Test void sameHashProducesSameMap() throws Exception {
        GameMap a = maps.generate("seed42", SizePreset.TINY);
        GameMap b = maps.generate("seed42", SizePreset.TINY);
        assertThat(a.totalFloorTiles()).isEqualTo(b.totalFloorTiles());
        assertThat(a.startX()).isEqualTo(b.startX());
        assertThat(a.startY()).isEqualTo(b.startY());
        for (int y = 0; y < a.height(); y++)
            for (int x = 0; x < a.width(); x++)
                assertThat(a.isFloor(x, y)).isEqualTo(b.isFloor(x, y));
    }

    @Test void differentHashesDifferentMaps() throws Exception {
        GameMap a = maps.generate("hashA", SizePreset.SMALL);
        GameMap b = maps.generate("hashB", SizePreset.SMALL);
        boolean differs = false;
        outer:
        for (int y = 0; y < a.height(); y++)
            for (int x = 0; x < a.width(); x++)
                if (a.isFloor(x, y) != b.isFloor(x, y)) { differs = true; break outer; }
        assertThat(differs).isTrue();
    }

    @Test void startTileIsFloor() throws Exception {
        GameMap map = maps.generate("test", SizePreset.SMALL);
        assertThat(map.isFloor(map.startX(), map.startY())).isTrue();
    }

    @Test void atLeastTwentyPercentFloor() throws Exception {
        GameMap map = maps.generate("coverage", SizePreset.MEDIUM);
        int total = map.width() * map.height();
        assertThat(map.totalFloorTiles()).isGreaterThan(total / 5);
    }

    @Test void allFloorTilesReachableFromStart() throws Exception {
        GameMap map = maps.generate("reach", SizePreset.SMALL);
        // BFS from start tile
        boolean[][] visited = new boolean[map.height()][map.width()];
        java.util.Queue<int[]> queue = new java.util.ArrayDeque<>();
        queue.add(new int[]{map.startX(), map.startY()});
        visited[map.startY()][map.startX()] = true;
        int reachable = 0;
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            reachable++;
            for (com.jacuum.algo.Direction d : com.jacuum.algo.Direction.values()) {
                int nx = cur[0] + d.dx(), ny = cur[1] + d.dy();
                if (map.isFloor(nx, ny) && !visited[ny][nx]) {
                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        assertThat(reachable).isEqualTo(map.totalFloorTiles());
    }
}
