package com.jacuum.engine

import com.jacuum.algo.Direction

data class IterationEvent(
    val sessionId: String,
    val iteration: Int,
    val direction: Direction?,
    val robotX: Int,
    val robotY: Int,
    val score: Int,
    val totalCleaned: Int,
    val totalFloor: Int,
    val finished: Boolean,
    val finishReason: FinishReason?
)
