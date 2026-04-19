package com.jacuum.algo

import com.jacuum.algo.impl.AlwaysLeftAlgo
import com.jacuum.algo.impl.FrontierAlgo
import com.jacuum.algo.impl.RandomAlgo
import com.jacuum.map.GeneratedMap
import com.jacuum.map.SizePreset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class AlgoSmokeTest {

    companion object {
        @JvmStatic
        fun algos(): Stream<RobotAlgo> = Stream.of(RandomAlgo(), AlwaysLeftAlgo(), FrontierAlgo())

        @JvmStatic
        fun squareMap(): com.jacuum.map.GameMap {
            val f = Array(7) { BooleanArray(7) }
            for (y in 1 until 6) for (x in 1 until 6) f[y][x] = true
            return GeneratedMap("sq", SizePreset.TINY, f, 3, 3)
        }

        @JvmStatic
        fun corridorMap(): com.jacuum.map.GameMap {
            val f = Array(3) { BooleanArray(9) }
            for (x in 1 until 8) f[1][x] = true
            return GeneratedMap("corr", SizePreset.TINY, f, 1, 1)
        }
    }

    @ParameterizedTest
    @MethodSource("algos")
    fun doesNotThrowOnSquareMap(algo: RobotAlgo) {
        runAlgo(algo, squareMap(), 50)
    }

    @ParameterizedTest
    @MethodSource("algos")
    fun doesNotThrowOnCorridor(algo: RobotAlgo) {
        runAlgo(algo, corridorMap(), 30)
    }

    private fun runAlgo(algo: RobotAlgo, map: com.jacuum.map.GameMap, iterations: Int) {
        var x = map.startX()
        var y = map.startY()
        val cleaned = HashSet<String>()
        for (i in 0 until iterations) {
            val fx = x; val fy = y
            val tile = object : Tile {
                override fun x(): Int = fx
                override fun y(): Int = fy
                override fun isClean(): Boolean = cleaned.contains("$fx,$fy")
                override fun hasWall(direction: Direction): Boolean = map.hasWall(fx, fy, direction)
            }
            val dir = algo.next(tile)
            assertThat(dir).isNotNull()
            if (!map.hasWall(x, y, dir)) {
                x += dir.dx()
                y += dir.dy()
            }
            cleaned.add("$x,$y")
        }
    }
}
