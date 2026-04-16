package com.jacuum.map;

import com.jacuum.algo.Direction;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GeneratedMapTest {

    private static boolean[][] smallFloor() {
        // 5x5 grid with 3x3 interior floor, border is wall (false)
        boolean[][] f = new boolean[5][5];
        for (int y = 1; y < 4; y++)
            for (int x = 1; x < 4; x++)
                f[y][x] = true;
        return f;
    }

    @Test void hashIsPreserved() {
        GameMap map = new GeneratedMap("myhash", SizePreset.TINY, smallFloor(), 2, 2);
        assertThat(map.hash()).isEqualTo("myhash");
    }

    @Test void totalFloorCountsOnlyTrueCells() {
        GameMap map = new GeneratedMap("h", SizePreset.TINY, smallFloor(), 2, 2);
        assertThat(map.totalFloorTiles()).isEqualTo(9); // 3x3 interior
    }

    @Test void borderIsWall() {
        GameMap map = new GeneratedMap("h", SizePreset.TINY, smallFloor(), 2, 2);
        // tile (1,1) has NORTH wall because (1,0) is not floor
        assertThat(map.hasWall(1, 1, Direction.NORTH)).isTrue();
        // tile (2,2) is interior — NORTH neighbor (2,1) is floor, so no wall
        assertThat(map.hasWall(2, 2, Direction.NORTH)).isFalse();
    }

    @Test void startTileIsFloor() {
        GameMap map = new GeneratedMap("h", SizePreset.TINY, smallFloor(), 2, 2);
        assertThat(map.isFloor(map.startX(), map.startY())).isTrue();
    }
}
