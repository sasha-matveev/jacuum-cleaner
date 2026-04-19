package com.jacuum.algo

enum class Direction {
    NORTH, SOUTH, EAST, WEST;

    fun opposite(): Direction = when (this) {
        NORTH -> SOUTH; SOUTH -> NORTH; EAST -> WEST; WEST -> EAST
    }
    fun dx(): Int = when (this) { EAST -> 1; WEST -> -1; else -> 0 }
    fun dy(): Int = when (this) { SOUTH -> 1; NORTH -> -1; else -> 0 }
}
