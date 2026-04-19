package com.jacuum.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.jacuum.leaderboard.LeaderboardEntry
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class LeaderboardEndpointTest @Autowired constructor(
    private val mvc: MockMvc,
    private val mapper: ObjectMapper
) {

    private fun entry() = LeaderboardEntry(
        "test-id", "TestPlayer", "\uD83E\uDD16",
        "abc123", "TINY", "Random",
        50, 100, 500,
        "2026-04-17T12:00:00Z",
        emptyList()
    )

    @Test fun leaderboardEndpointReturnsEmptyListByDefault() {
        mvc.perform(get("/api/leaderboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test fun leaderboardPostSavesEntryAndReturnsIt() {
        mvc.perform(
            post("/api/leaderboard")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(entry()))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("test-id"))
            .andExpect(jsonPath("$.username").value("TestPlayer"))
            .andExpect(jsonPath("$.mapHash").value("abc123"))
            .andExpect(jsonPath("$.mapSize").value("TINY"))
            .andExpect(jsonPath("$.algoName").value("Random"))
            .andExpect(jsonPath("$.iterationsUsed").value(50))
            .andExpect(jsonPath("$.iterationsAvailable").value(100))
            .andExpect(jsonPath("$.score").value(500))
            .andExpect(jsonPath("$.completedAt").value("2026-04-17T12:00:00Z"))
    }
}
