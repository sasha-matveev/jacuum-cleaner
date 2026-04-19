package com.jacuum.map

import com.jacuum.algo.Direction

class GeneratedMap(
    private val hash: String,
    private val size: SizePreset,
    private val floor: Array<BooleanArray>,
    private val startX: Int,
    private val startY: Int
) : GameMap {
    private val totalFloor: Int = floor.sumOf { row -> row.count { it } }

    override fun hash(): String = hash
    override fun size(): SizePreset = size
    override fun width(): Int = floor[0].size
    override fun height(): Int = floor.size
    override fun isFloor(x: Int, y: Int): Boolean = inBounds(x, y) && floor[y][x]
    override fun startX(): Int = startX
    override fun startY(): Int = startY
    override fun totalFloorTiles(): Int = totalFloor

    override fun hasWall(x: Int, y: Int, direction: Direction): Boolean {
        val nx = x + direction.dx()
        val ny = y + direction.dy()
        return !inBounds(nx, ny) || !floor[ny][nx]
    }

    private fun inBounds(x: Int, y: Int) = x >= 0 && y >= 0 && x < width() && y < height()
}
