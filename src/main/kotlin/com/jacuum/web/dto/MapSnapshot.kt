package com.jacuum.web.dto

data class MapSnapshot(
    val width: Int,
    val height: Int,
    val startX: Int,
    val startY: Int,
    val totalFloor: Int,
    val tiles: List<TileSnapshot>
) {
    data class TileSnapshot(
        val x: Int, val y: Int,
        val wallNorth: Boolean, val wallSouth: Boolean,
        val wallEast: Boolean, val wallWest: Boolean
    )
}
