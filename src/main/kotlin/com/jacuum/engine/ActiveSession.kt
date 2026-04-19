package com.jacuum.engine

import com.jacuum.map.GameMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

internal class ActiveSession(
    val id: String,
    val map: GameMap,
    val algoName: String,
    val username: String,
    val avatar: String,
    val iterationsAvailable: Int
) {
    @Volatile var robotX: Int = map.startX()
    @Volatile var robotY: Int = map.startY()
    @Volatile var score: Int = 0
    @Volatile var iterationsUsed: Int = 0
    @Volatile var status: RunStatus = RunStatus.SETUP
    @Volatile var finishReason: FinishReason? = null
    val cleaned: MutableSet<String> = ConcurrentHashMap.newKeySet()
    var job: Job? = null
    // Unlocked = running; locked = paused. Game loop calls withLock{} to suspend while paused.
    val pauseGate: Mutex = Mutex()

    fun toView() = SessionView(
        id, status, robotX, robotY, score,
        cleaned.size, iterationsUsed, iterationsAvailable,
        map.totalFloorTiles(), finishReason
    )
}
