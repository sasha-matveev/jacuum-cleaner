package com.jacuum.map

import com.jacuum.algo.Direction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GeneratedMapTest {

    private fun smallFloor(): Array<BooleanArray> {
        val f = Array(5) { BooleanArray(5) }
        for (y in 1 until 4) for (x in 1 until 4) f[y][x] = true
        return f
    }

    @Test fun hashIsPreserved() {
        val map: GameMap = GeneratedMap("myhash", SizePreset.TINY, smallFloor(), 2, 2)
        assertThat(map.hash()).isEqualTo("myhash")
    }

    @Test fun totalFloorCountsOnlyTrueCells() {
        val map: GameMap = GeneratedMap("h", SizePreset.TINY, smallFloor(), 2, 2)
        assertThat(map.totalFloorTiles()).isEqualTo(9)
    }

    @Test fun borderIsWall() {
        val map: GameMap = GeneratedMap("h", SizePreset.TINY, smallFloor(), 2, 2)
        assertThat(map.hasWall(1, 1, Direction.NORTH)).isTrue()
        assertThat(map.hasWall(2, 2, Direction.NORTH)).isFalse()
    }

    @Test fun startTileIsFloor() {
        val map: GameMap = GeneratedMap("h", SizePreset.TINY, smallFloor(), 2, 2)
        assertThat(map.isFloor(map.startX(), map.startY())).isTrue()
    }
}
