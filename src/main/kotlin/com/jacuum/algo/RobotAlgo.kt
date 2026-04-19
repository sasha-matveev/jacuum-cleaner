package com.jacuum.algo

fun interface RobotAlgo {
    @Throws(Exception::class)
    fun next(tile: Tile): Direction
}
