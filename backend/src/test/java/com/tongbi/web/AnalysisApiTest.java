package com.tongbi.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void sampleEndpointStripsQuizAnswers() throws Exception {
        MvcResult result = mvc.perform(post("/api/analyses/sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score.total").value(47))
                .andExpect(jsonPath("$.listScore").value(52))
                .andExpect(jsonPath("$.quiz[0].answer").doesNotExist())
                .andExpect(jsonPath("$.quiz[0].options[0].value").value("empty"))
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/api/analyses/" + id + "/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("{\"answers\":{\"q-empty\":\"throw\",\"q-status\":\"throw\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreText").value("2/2"))
                .andExpect(jsonPath("$.answer").doesNotExist());
    }
}
