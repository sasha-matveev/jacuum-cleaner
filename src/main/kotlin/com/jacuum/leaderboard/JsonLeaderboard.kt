package com.jacuum.leaderboard

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path

class JsonLeaderboard(private val file: Path) : Leaderboard {

    private val mapper = jacksonObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)

    @Synchronized
    override fun entries(): List<LeaderboardEntry> {
        if (!Files.exists(file)) return emptyList()
        return mapper.readValue(file.toFile())
    }

    @Synchronized
    override fun save(entry: LeaderboardEntry) {
        val current = entries().toMutableList()
        current.add(entry)
        mapper.writeValue(file.toFile(), current)
    }
}
