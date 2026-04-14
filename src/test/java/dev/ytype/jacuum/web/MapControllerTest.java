package dev.ytype.jacuum.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.ytype.jacuum.domain.SizePreset;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MapControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MapController()).build();

    @Test
    void explicitHashAndSmallPresetReturnTheSameJsonOnRepeatedCalls() throws Exception {
        String requestBody = """
                {"hash":"demo","size":"small"}
                """;

        String first = readResponse(requestBody);
        String second = readResponse(requestBody);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void explicitHashAndSmallPresetReturnExpectedShape() throws Exception {
        mockMvc.perform(post("/api/maps")
                        .contentType("application/json")
                        .content("""
                                {"hash":"demo","size":"small"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hash").value("demo"))
                .andExpect(jsonPath("$.size").value("small"))
                .andExpect(jsonPath("$.width").value(SizePreset.SMALL.width()))
                .andExpect(jsonPath("$.height").value(SizePreset.SMALL.height()))
                .andExpect(jsonPath("$.start.x").isNumber())
                .andExpect(jsonPath("$.start.y").isNumber())
                .andExpect(jsonPath("$.tileMatrix.length()").value(SizePreset.SMALL.height()))
                .andExpect(jsonPath("$.tileMatrix[0].length()").value(SizePreset.SMALL.width()))
                .andExpect(jsonPath("$.tileMatrix[0][0]").value(anyOf(is("floor"), is("wall"))));
    }

    @Test
    void requestWithoutHashReturnsANonBlankGeneratedHash() throws Exception {
        String response = readResponse("""
                {"size":"small"}
                """);

        assertThat(response).containsPattern("\"hash\":\"[^\"]+\"");
    }

    @Test
    void blankHashReturnsANonBlankGeneratedHash() throws Exception {
        String response = readResponse("""
                {"hash":"   ","size":"small"}
                """);

        assertThat(response).containsPattern("\"hash\":\"[^\"]+\"");
    }

    @Test
    void missingSizeReturnsBadRequest() throws Exception {
        assertBadRequest("""
                {"hash":"demo"}
                """);
    }

    @Test
    void blankSizeReturnsBadRequest() throws Exception {
        assertBadRequest("""
                {"hash":"demo","size":"   "}
                """);
    }

    @Test
    void unknownPresetReturnsBadRequest() throws Exception {
        assertBadRequest("""
                {"hash":"demo","size":"giant"}
                """);
    }

    @Test
    void nullBodyReturnsBadRequest() throws Exception {
        assertBadRequest("null");
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/maps")
                        .contentType("application/json")
                        .content("""
                                {"size":"small"
                                """))
                .andExpect(status().isBadRequest());
    }

    private void assertBadRequest(String requestBody) throws Exception {
        mockMvc.perform(post("/api/maps")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    private String readResponse(String requestBody) throws Exception {
        return mockMvc.perform(post("/api/maps")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
