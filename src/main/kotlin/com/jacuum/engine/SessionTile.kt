package com.jacuum.engine

import com.jacuum.algo.Direction
import com.jacuum.algo.Tile

internal class SessionTile(
    private val x: Int,
    private val y: Int,
    private val map: com.jacuum.map.GameMap,
    private val cleaned: Set<String>
) : Tile {
    override fun x(): Int = x
    override fun y(): Int = y
    override fun isClean(): Boolean = cleaned.contains("$x,$y")
    override fun hasWall(direction: Direction): Boolean = map.hasWall(x, y, direction)
}
