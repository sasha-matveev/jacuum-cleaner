package com.jacuum.engine

import com.jacuum.algo.Algorithms
import com.jacuum.algo.Direction
import com.jacuum.algo.RobotAlgo
import com.jacuum.map.GameMap
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MemorySessions(
    private val messaging: Messaging,
    private val algorithms: Algorithms?,
    private val maxSessions: Int
) : Sessions {

    private val store = ConcurrentHashMap<String, ActiveSession>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @PreDestroy
    fun destroy() = scope.cancel()

    override fun open(
        map: GameMap, algoName: String, username: String,
        avatar: String, iterations: Int
    ): String {
        if (store.size >= maxSessions)
            throw Exception("Session cap reached ($maxSessions max)")
        val id = UUID.randomUUID().toString()
        store[id] = ActiveSession(id, map, algoName, username, avatar, iterations)
        return id
    }

    override fun view(id: String): SessionView = require(id).toView()

    override fun start(id: String) {
        val s = require(id)
        synchronized(s) {
            if (s.status != RunStatus.SETUP && s.status != RunStatus.PAUSED)
                throw Exception("Cannot start session in state: ${s.status}")
            s.status = RunStatus.RUNNING
        }
        val alg = algorithms ?: throw Exception("No algorithms configured")
        val algo = alg.instantiate(s.algoName)
        s.job = scope.launch { runLoop(s, algo) }
    }

    private suspend fun runLoop(s: ActiveSession, algo: RobotAlgo) {
        while (s.iterationsUsed < s.iterationsAvailable && s.status != RunStatus.FINISHED) {
            coroutineContext.ensureActive()
            s.pauseGate.withLock {} // suspends here while paused

            val tile = SessionTile(s.robotX, s.robotY, s.map, s.cleaned)
            val dir: Direction = try {
                algo.next(tile)
            } catch (e: Exception) {
                finish(s, FinishReason.ALGO_CRASH); return
            }
            val moved = !s.map.hasWall(s.robotX, s.robotY, dir)
            if (moved) { s.robotX += dir.dx(); s.robotY += dir.dy() }
            if (s.cleaned.add("${s.robotX},${s.robotY}")) s.score += 100
            s.iterationsUsed++

            messaging.send(
                "/topic/session/${s.id}/events",
                IterationEvent(
                    s.id, s.iterationsUsed, if (moved) dir else null,
                    s.robotX, s.robotY, s.score,
                    s.cleaned.size, s.map.totalFloorTiles(), false, null
                )
            )

            if (s.cleaned.size == s.map.totalFloorTiles()) {
                finish(s, FinishReason.COMPLETED); return
            }
        }
        if (s.status != RunStatus.FINISHED) finish(s, FinishReason.OUT_OF_ITERATIONS)
    }

    private fun finish(s: ActiveSession, reason: FinishReason) {
        s.status = RunStatus.FINISHED
        s.finishReason = reason
        if (reason == FinishReason.ALGO_CRASH) s.score = 0
        messaging.send(
            "/topic/session/${s.id}/events",
            IterationEvent(
                s.id, s.iterationsUsed, null,
                s.robotX, s.robotY, s.score,
                s.cleaned.size, s.map.totalFloorTiles(), true, reason
            )
        )
        messaging.send(
            "/topic/session/${s.id}/status",
            StatusEvent(s.id, RunStatus.FINISHED, reason)
        )
    }

    override fun pause(id: String) {
        val s = require(id)
        synchronized(s) {
            if (s.status != RunStatus.RUNNING)
                throw Exception("Cannot pause session in state: ${s.status}")
            s.status = RunStatus.PAUSED
        }
        s.pauseGate.tryLock() // takes the gate; loop suspends on next withLock{}
    }

    override fun resume(id: String) {
        val s = require(id)
        synchronized(s) {
            if (s.status != RunStatus.PAUSED)
                throw Exception("Cannot resume session in state: ${s.status}")
            s.status = RunStatus.RUNNING
        }
        if (s.pauseGate.isLocked) s.pauseGate.unlock()
    }

    override fun stop(id: String) {
        val s = require(id)
        synchronized(s) {
            if (s.status == RunStatus.FINISHED) return
            finish(s, FinishReason.INTERRUPTED)
        }
        if (s.pauseGate.isLocked) s.pauseGate.unlock()
        s.job?.cancel()
    }

    internal fun require(id: String): ActiveSession =
        Optional.ofNullable(store[id])
            .orElseThrow { Exception("Unknown session: $id") }
}
