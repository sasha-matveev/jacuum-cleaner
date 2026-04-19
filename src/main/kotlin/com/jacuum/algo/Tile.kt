package com.jacuum.algo

interface Tile {
    fun x(): Int
    fun y(): Int
    fun isClean(): Boolean
    fun hasWall(direction: Direction): Boolean
}
