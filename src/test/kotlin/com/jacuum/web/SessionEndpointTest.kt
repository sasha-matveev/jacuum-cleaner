package com.jacuum.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.jacuum.web.dto.CreateSessionRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class SessionEndpointTest @Autowired constructor(
    private val mvc: MockMvc,
    private val mapper: ObjectMapper
) {

    private fun createSession(body: Any) = mvc.perform(
        post("/api/session")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(body))
    )

    @Test fun createSessionReturns200WithSessionId() {
        createSession(CreateSessionRequest(null, "TINY", "Random", "Alice", "\uD83E\uDD16", 100))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").isNotEmpty())
            .andExpect(jsonPath("$.status").value("SETUP"))
            .andExpect(jsonPath("$.map.width").isNumber())
    }

    @Test fun startSessionReturns200() {
        val resp = createSession(CreateSessionRequest(null, "TINY", "Random", "Alice", "\uD83E\uDD16", 50))
            .andReturn().response.contentAsString
        val id = mapper.readTree(resp).get("sessionId").asText()
        mvc.perform(post("/api/session/$id/start"))
            .andExpect(status().isOk())
    }

    @Test fun createSessionWithMissingAlgoNameReturns400() {
        createSession(CreateSessionRequest(null, "TINY", null, "Alice", "\uD83E\uDD16", 100))
            .andExpect(status().isBadRequest())
    }

    @Test fun createSessionWithMissingUsernameReturns400() {
        createSession(CreateSessionRequest(null, "TINY", "Random", null, "\uD83E\uDD16", 100))
            .andExpect(status().isBadRequest())
    }

    @Test fun createSessionWithMissingAvatarReturns400() {
        createSession(CreateSessionRequest(null, "TINY", "Random", "Alice", null, 100))
            .andExpect(status().isBadRequest())
    }

    @Test fun createSessionWithBlankAlgoNameReturns400() {
        createSession(CreateSessionRequest(null, "TINY", "  ", "Alice", "\uD83E\uDD16", 100))
            .andExpect(status().isBadRequest())
    }

    @Test fun createSessionWithBlankUsernameReturns400() {
        createSession(CreateSessionRequest(null, "TINY", "Random", "  ", "\uD83E\uDD16", 100))
            .andExpect(status().isBadRequest())
    }

    @Test fun createSessionWithBlankAvatarReturns400() {
        createSession(CreateSessionRequest(null, "TINY", "Random", "Alice", "  ", 100))
            .andExpect(status().isBadRequest())
    }
}
