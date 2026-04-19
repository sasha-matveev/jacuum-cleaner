package com.jacuum.web.dto

data class SessionResponse(
    val sessionId: String,
    val status: String,
    val map: MapSnapshot,
    val robotX: Int,
    val robotY: Int,
    val totalFloor: Int,
    val iterationsAvailable: Int
)
