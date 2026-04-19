package com.jacuum.map

import com.jacuum.algo.Direction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.ArrayDeque

class CellularMapsTest {

    private val maps: Maps = CellularMaps()

    @Test fun sameHashProducesSameMap() {
        val a = maps.generate("seed42", SizePreset.TINY)
        val b = maps.generate("seed42", SizePreset.TINY)
        assertThat(a.totalFloorTiles()).isEqualTo(b.totalFloorTiles())
        assertThat(a.startX()).isEqualTo(b.startX())
        assertThat(a.startY()).isEqualTo(b.startY())
        for (y in 0 until a.height())
            for (x in 0 until a.width())
                assertThat(a.isFloor(x, y)).isEqualTo(b.isFloor(x, y))
    }

    @Test fun differentHashesDifferentMaps() {
        val a = maps.generate("hashA", SizePreset.SMALL)
        val b = maps.generate("hashB", SizePreset.SMALL)
        var differs = false
        outer@ for (y in 0 until a.height())
            for (x in 0 until a.width())
                if (a.isFloor(x, y) != b.isFloor(x, y)) { differs = true; break@outer }
        assertThat(differs).isTrue()
    }

    @Test fun startTileIsFloor() {
        val map = maps.generate("test", SizePreset.SMALL)
        assertThat(map.isFloor(map.startX(), map.startY())).isTrue()
    }

    @Test fun atLeastTwentyPercentFloor() {
        val map = maps.generate("coverage", SizePreset.MEDIUM)
        val total = map.width() * map.height()
        assertThat(map.totalFloorTiles()).isGreaterThan(total / 5)
    }

    @Test fun allFloorTilesReachableFromStart() {
        val map = maps.generate("reach", SizePreset.SMALL)
        val visited = Array(map.height()) { BooleanArray(map.width()) }
        val queue = ArrayDeque<IntArray>()
        queue.add(intArrayOf(map.startX(), map.startY()))
        visited[map.startY()][map.startX()] = true
        var reachable = 0
        while (queue.isNotEmpty()) {
            val cur = queue.poll()
            reachable++
            for (d in Direction.entries) {
                val nx = cur[0] + d.dx(); val ny = cur[1] + d.dy()
                if (map.isFloor(nx, ny) && !visited[ny][nx]) {
                    visited[ny][nx] = true
                    queue.add(intArrayOf(nx, ny))
                }
            }
        }
        assertThat(reachable).isEqualTo(map.totalFloorTiles())
    }
}
