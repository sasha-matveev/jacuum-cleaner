package com.jacuum.web

import com.jacuum.engine.Sessions
import com.jacuum.engine.SessionView
import com.jacuum.map.GameMap
import com.jacuum.map.Maps
import com.jacuum.map.SizePreset
import com.jacuum.web.dto.CreateSessionRequest
import com.jacuum.web.dto.SessionResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/session")
class SessionEndpoint(
    private val sessions: Sessions,
    private val maps: Maps,
    private val snapshots: Snapshots,
    @Value("\${game.default-iterations:500}") private val defaultIterations: Int
) : SessionApi {

    @PostMapping
    override fun create(@RequestBody req: CreateSessionRequest): SessionResponse {
        if (req.algoName.isNullOrBlank())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "algoName is required")
        if (req.username.isNullOrBlank())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required")
        if (req.avatar.isNullOrBlank())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "avatar is required")
        val hash = req.hash?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val size = sizeFrom(req.size)
        val iters = if (req.iterations > 0) req.iterations else defaultIterations
        val map = maps.generate(hash, size)
        val id = sessions.open(map, req.algoName, req.username, req.avatar, iters)
        return toResponse(id, map, iters)
    }

    @PostMapping("/{id}/start")
    override fun start(@PathVariable id: String): SessionView {
        sessions.start(id); return sessions.view(id)
    }

    @PostMapping("/{id}/pause")
    override fun pause(@PathVariable id: String): SessionView {
        sessions.pause(id); return sessions.view(id)
    }

    @PostMapping("/{id}/resume")
    override fun resume(@PathVariable id: String): SessionView {
        sessions.resume(id); return sessions.view(id)
    }

    @PostMapping("/{id}/stop")
    override fun stop(@PathVariable id: String): SessionView {
        sessions.stop(id); return sessions.view(id)
    }

    @GetMapping("/{id}")
    override fun view(@PathVariable id: String): SessionView = sessions.view(id)

    private fun toResponse(id: String, map: GameMap, iters: Int) =
        SessionResponse(id, "SETUP", snapshots.of(map),
            map.startX(), map.startY(), map.totalFloorTiles(), iters)

    private fun sizeFrom(s: String?): SizePreset {
        if (s.isNullOrBlank()) return SizePreset.SMALL
        return try { SizePreset.valueOf(s.uppercase()) }
        catch (e: IllegalArgumentException) { SizePreset.SMALL }
    }
}
