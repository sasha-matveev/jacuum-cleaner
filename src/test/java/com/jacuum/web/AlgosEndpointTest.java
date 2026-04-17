package com.jacuum.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AlgosEndpointTest {

    private final MockMvc mvc;

    @Autowired
    AlgosEndpointTest(final MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test void algosEndpointReturnsList() throws Exception {
        mvc.perform(get("/api/algos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test void avatarsEndpointReturnsList() throws Exception {
        mvc.perform(get("/api/avatars"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
