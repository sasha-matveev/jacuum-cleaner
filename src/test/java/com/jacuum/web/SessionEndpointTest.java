package com.jacuum.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jacuum.web.dto.CreateSessionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SessionEndpointTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void createSessionReturns200WithSessionId() throws Exception {
        var body = new CreateSessionRequest(null, "TINY", "Random", "Alice", "🤖", 100);
        mvc.perform(post("/api/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").isNotEmpty())
            .andExpect(jsonPath("$.status").value("SETUP"))
            .andExpect(jsonPath("$.map.width").isNumber());
    }

    @Test void startSessionReturns200() throws Exception {
        // create first
        var body = new CreateSessionRequest(null, "TINY", "Random", "Alice", "🤖", 50);
        String resp = mvc.perform(post("/api/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(resp).get("sessionId").asText();

        mvc.perform(post("/api/session/" + id + "/start"))
            .andExpect(status().isOk());
    }

    @Test void createSessionWithMissingAlgoNameReturns400() throws Exception {
        var body = new CreateSessionRequest(null, "TINY", null, "Alice", "🤖", 100);
        mvc.perform(post("/api/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }

    @Test void createSessionWithMissingUsernameReturns400() throws Exception {
        var body = new CreateSessionRequest(null, "TINY", "Random", null, "🤖", 100);
        mvc.perform(post("/api/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }

    @Test void createSessionWithMissingAvatarReturns400() throws Exception {
        var body = new CreateSessionRequest(null, "TINY", "Random", "Alice", null, 100);
        mvc.perform(post("/api/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }

    @Test void createSessionWithBlankAlgoNameReturns400() throws Exception {
        var body = new CreateSessionRequest(null, "TINY", "  ", "Alice", "🤖", 100);
        mvc.perform(post("/api/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }

    @Test void createSessionWithBlankUsernameReturns400() throws Exception {
        var body = new CreateSessionRequest(null, "TINY", "Random", "  ", "🤖", 100);
        mvc.perform(post("/api/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }

    @Test void createSessionWithBlankAvatarReturns400() throws Exception {
        var body = new CreateSessionRequest(null, "TINY", "Random", "Alice", "  ", 100);
        mvc.perform(post("/api/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }
}
