package com.jacuum.engine

data class SessionView(
    val id: String,
    val status: RunStatus,
    val robotX: Int,
    val robotY: Int,
    val score: Int,
    val totalCleaned: Int,
    val iterationsUsed: Int,
    val iterationsAvailable: Int,
    val totalFloor: Int,
    val finishReason: FinishReason?
)
