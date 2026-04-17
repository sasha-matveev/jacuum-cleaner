package com.jacuum.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jacuum.leaderboard.LeaderboardEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LeaderboardEndpointTest {

    private final MockMvc mvc;
    private final ObjectMapper mapper;

    @Autowired
    LeaderboardEndpointTest(final MockMvc mvc, final ObjectMapper mapper) {
        this.mvc = mvc;
        this.mapper = mapper;
    }

    @Test void leaderboardEndpointReturnsEmptyListByDefault() throws Exception {
        mvc.perform(get("/api/leaderboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test void leaderboardPostSavesEntryAndReturnsIt() throws Exception {
        var entry = new LeaderboardEntry(
            "test-id",
            "TestPlayer",
            "🤖",
            "abc123",
            "TINY",
            "Random",
            50,
            100,
            500,
            "2026-04-17T12:00:00Z",
            List.of()
        );

        mvc.perform(post("/api/leaderboard")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(entry)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("test-id"))
            .andExpect(jsonPath("$.username").value("TestPlayer"))
            .andExpect(jsonPath("$.avatar").value("🤖"))
            .andExpect(jsonPath("$.mapHash").value("abc123"))
            .andExpect(jsonPath("$.mapSize").value("TINY"))
            .andExpect(jsonPath("$.algoName").value("Random"))
            .andExpect(jsonPath("$.iterationsUsed").value(50))
            .andExpect(jsonPath("$.iterationsAvailable").value(100))
            .andExpect(jsonPath("$.score").value(500))
            .andExpect(jsonPath("$.completedAt").value("2026-04-17T12:00:00Z"));
    }
}
