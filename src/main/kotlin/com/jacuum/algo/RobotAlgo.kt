package com.jacuum.algo

interface RobotAlgo {
    @Throws(Exception::class)
    fun next(tile: Tile): Direction
}
