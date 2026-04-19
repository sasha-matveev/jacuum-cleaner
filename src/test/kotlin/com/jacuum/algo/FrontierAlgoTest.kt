package com.jacuum.algo

import com.jacuum.algo.impl.FrontierAlgo
import com.jacuum.map.GeneratedMap
import com.jacuum.map.SizePreset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FrontierAlgoTest {

    private fun openRoom(): com.jacuum.map.GameMap {
        val f = Array(7) { BooleanArray(7) }
        for (y in 1..5) for (x in 1..5) f[y][x] = true
        return GeneratedMap("open5x5", SizePreset.TINY, f, 3, 3)
    }

    private fun corridor(): com.jacuum.map.GameMap {
        val f = Array(3) { BooleanArray(9) }
        for (x in 1..7) f[1][x] = true
        return GeneratedMap("corr7", SizePreset.TINY, f, 4, 1)
    }

    private fun lShape(): com.jacuum.map.GameMap {
        val f = Array(5) { BooleanArray(5) }
        for (y in 1..3) f[y][1] = true
        for (x in 1..3) f[3][x] = true
        return GeneratedMap("lshape", SizePreset.TINY, f, 1, 1)
    }

    private fun simulate(map: com.jacuum.map.GameMap, maxIter: Int): Int {
        val algo: RobotAlgo = FrontierAlgo()
        var x = map.startX(); var y = map.startY()
        val cleaned = HashSet<String>()

        for (i in 0 until maxIter) {
            val fx = x; val fy = y
            val snap = cleaned.toSet()
            val tile = object : Tile {
                override fun x(): Int = fx
                override fun y(): Int = fy
                override fun isClean(): Boolean = snap.contains("$fx,$fy")
                override fun hasWall(direction: Direction): Boolean = map.hasWall(fx, fy, direction)
            }
            val dir = algo.next(tile)
            assertThat(dir).`as`("algo returned null on iteration %d", i).isNotNull()

            if (!map.hasWall(x, y, dir)) { x += dir.dx(); y += dir.dy() }
            cleaned.add("$x,$y")

            if (cleaned.size == map.totalFloorTiles()) break
        }
        return cleaned.size
    }

    @Test fun coversEveryTileInOpenRoom() {
        assertThat(simulate(openRoom(), 200)).isEqualTo(openRoom().totalFloorTiles())
    }

    @Test fun coversEveryTileInCorridor() {
        assertThat(simulate(corridor(), 20)).isEqualTo(corridor().totalFloorTiles())
    }

    @Test fun coversEveryTileInLShape() {
        assertThat(simulate(lShape(), 30)).isEqualTo(lShape().totalFloorTiles())
    }

    @Test fun neverReturnsNullDirection() {
        simulate(openRoom(), 50)
    }

    @Test fun doesNotWasteIterationsBumpingWalls() {
        assertThat(simulate(corridor(), 15)).isEqualTo(corridor().totalFloorTiles())
    }
}
