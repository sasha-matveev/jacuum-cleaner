package com.jacuum.map

import com.jacuum.algo.Direction
import java.util.ArrayDeque
import java.util.Random

class CellularMaps : Maps {

    private val smoothingPasses = 5
    private val fillRatio = 0.45

    override fun generate(hash: String, size: SizePreset): GameMap {
        val rng = Random(seedFrom(hash))
        val w = size.width; val h = size.height
        var floor = initialFloor(rng, w, h)
        repeat(smoothingPasses) { floor = smooth(floor, w, h) }
        floor = keepLargestRegion(floor, w, h)
        val (sx, sy) = centroidOfFloor(floor, w, h)
        return GeneratedMap(hash, size, floor, sx, sy)
    }

    private fun seedFrom(hash: String): Long {
        var h = -3750763034362895579L
        for (c in hash) h = (h xor c.code.toLong()) * 1099511628211L
        return h
    }

    private fun initialFloor(rng: Random, w: Int, h: Int): Array<BooleanArray> {
        val f = Array(h) { BooleanArray(w) }
        for (y in 1 until h - 1)
            for (x in 1 until w - 1)
                f[y][x] = rng.nextDouble() > fillRatio
        return f
    }

    private fun smooth(f: Array<BooleanArray>, w: Int, h: Int): Array<BooleanArray> {
        val next = Array(h) { BooleanArray(w) }
        for (y in 1 until h - 1)
            for (x in 1 until w - 1) {
                var walls = 0
                for (dy in -1..1) for (dx in -1..1) if (!f[y + dy][x + dx]) walls++
                next[y][x] = walls < 5
            }
        return next
    }

    private fun keepLargestRegion(floor: Array<BooleanArray>, w: Int, h: Int): Array<BooleanArray> {
        val visited = Array(h) { BooleanArray(w) }
        val regions = mutableListOf<List<Pair<Int, Int>>>()
        for (y in 0 until h) for (x in 0 until w) {
            if (floor[y][x] && !visited[y][x]) {
                val region = mutableListOf<Pair<Int, Int>>()
                val q = ArrayDeque<Pair<Int, Int>>()
                q.add(x to y); visited[y][x] = true
                while (q.isNotEmpty()) {
                    val (cx, cy) = q.poll(); region.add(cx to cy)
                    for (d in Direction.values()) {
                        val nx = cx + d.dx(); val ny = cy + d.dy()
                        if (nx in 0 until w && ny in 0 until h && floor[ny][nx] && !visited[ny][nx]) {
                            visited[ny][nx] = true; q.add(nx to ny)
                        }
                    }
                }
                regions.add(region)
            }
        }
        if (regions.isEmpty()) {
            val fallback = Array(h) { BooleanArray(w) }
            val cx = w / 2; val cy = h / 2
            for (dy in -1..1) for (dx in -1..1) fallback[cy + dy][cx + dx] = true
            return fallback
        }
        val largest = regions.maxByOrNull { it.size }!!
        val result = Array(h) { BooleanArray(w) }
        for ((rx, ry) in largest) result[ry][rx] = true
        return result
    }

    private fun centroidOfFloor(floor: Array<BooleanArray>, w: Int, h: Int): Pair<Int, Int> {
        var sx = 0L; var sy = 0L; var count = 0L
        for (y in 0 until h) for (x in 0 until w) if (floor[y][x]) { sx += x; sy += y; count++ }
        if (count == 0L) return w / 2 to h / 2
        val cx = (sx / count).toInt(); val cy = (sy / count).toInt()
        var best = Int.MAX_VALUE; var result = cx to cy
        for (y in 0 until h) for (x in 0 until w) if (floor[y][x]) {
            val dist = (x - cx) * (x - cx) + (y - cy) * (y - cy)
            if (dist < best) { best = dist; result = x to y }
        }
        return result
    }
}
