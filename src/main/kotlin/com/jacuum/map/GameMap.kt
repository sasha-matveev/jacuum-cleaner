package com.jacuum.map

import com.jacuum.algo.Direction

interface GameMap {
    fun hash(): String
    fun size(): SizePreset
    fun width(): Int
    fun height(): Int
    fun isFloor(x: Int, y: Int): Boolean
    fun hasWall(x: Int, y: Int, direction: Direction): Boolean
    fun startX(): Int
    fun startY(): Int
    fun totalFloorTiles(): Int
}
