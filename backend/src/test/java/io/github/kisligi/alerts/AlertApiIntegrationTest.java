package io.github.kisligi.alerts;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end through the real stack: HTTP layer, service, JPA, Flyway migrations and a real
 * PostgreSQL started by Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AlertApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void seedsDemoUsers() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Alice Analyst", "Bob Trader", "Carol Responder")));
    }

    @Test
    void alertRuleLifecycle() throws Exception {
        String created = mvc.perform(post("/api/users/2/alerts").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "name": "Market crashes",
                          "categories": ["MARKET", "NEWS"],
                          "keywords": ["Crash", "sell-off"],
                          "minSeverity": "HIGH",
                          "targets": [
                            {"type": "SLACK", "address": "http://slack-mock:8080/services/bob"},
                            {"type": "EMAIL", "address": "bob@example.com"}
                          ]
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.categories", contains("NEWS", "MARKET")))
                .andExpect(jsonPath("$.keywords", contains("crash", "sell-off")))
                .andExpect(jsonPath("$.targets[0].type").value("EMAIL"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.id");

        mvc.perform(get("/api/users/2/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(put("/api/alerts/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "name": "Market crashes (critical only)",
                          "categories": ["MARKET"],
                          "minSeverity": "CRITICAL",
                          "targets": [{"type": "EMAIL", "address": "bob@example.com"}],
                          "enabled": false
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minSeverity").value("CRITICAL"))
                .andExpect(jsonPath("$.keywords", hasSize(0)))
                .andExpect(jsonPath("$.targets", hasSize(1)))
                .andExpect(jsonPath("$.enabled").value(false));

        mvc.perform(delete("/api/alerts/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/alerts/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void unknownUserIs404() throws Exception {
        mvc.perform(get("/api/users/999/alerts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("User 999 not found"));
    }

    @Test
    void publishesOpenApiDocument() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/users/{userId}/alerts']").exists());
    }
}
