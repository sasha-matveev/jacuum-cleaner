package com.jacuum.leaderboard

import com.jacuum.algo.Direction

data class TraceEvent(
    val iteration: Int,
    val direction: Direction?,
    val x: Int,
    val y: Int,
    val score: Int
)
