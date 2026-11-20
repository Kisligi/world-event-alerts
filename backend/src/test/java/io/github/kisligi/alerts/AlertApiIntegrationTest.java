package io.github.kisligi.alerts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end through the real stack: HTTP layer, service, JPA, Flyway migrations and a real
 * PostgreSQL started by Testcontainers. Each test starts with no alert rules; background event
 * polling is off.
 */
@SpringBootTest(properties = "alerts.sources.polling-enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AlertApiIntegrationTest {

    private static final String BOB_RULE = """
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
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void deleteAllAlertRules() {
        jdbc.update("DELETE FROM alert_rule");
    }

    @Test
    void seedsDemoUsers() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Alice Analyst", "Bob Trader", "Carol Responder")));
    }

    @Test
    void alertRuleLifecycle() throws Exception {
        String created = mvc.perform(post("/api/users/2/alerts")
                        .contentType(MediaType.APPLICATION_JSON).content(BOB_RULE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.categories", contains("NEWS", "MARKET")))
                .andExpect(jsonPath("$.keywords", contains("crash", "sell-off")))
                .andExpect(jsonPath("$.targets[0].type").value("EMAIL"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.id");
        Instant createdAt = Instant.parse(JsonPath.read(created, "$.createdAt"));

        mvc.perform(get("/api/alerts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Market crashes"))
                .andExpect(jsonPath("$.targets", hasSize(2)));

        String updated = mvc.perform(put("/api/alerts/" + id).contentType(MediaType.APPLICATION_JSON).content("""
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
                .andExpect(jsonPath("$.enabled").value(false))
                .andReturn().getResponse().getContentAsString();
        assertThat(Instant.parse(JsonPath.<String>read(updated, "$.createdAt"))).isEqualTo(createdAt);
        assertThat(Instant.parse(JsonPath.<String>read(updated, "$.updatedAt"))).isAfter(createdAt);

        mvc.perform(delete("/api/alerts/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/alerts/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void deletingRuleLeavesNoOrphanRows() throws Exception {
        String created = mvc.perform(post("/api/users/2/alerts")
                        .contentType(MediaType.APPLICATION_JSON).content(BOB_RULE))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.id");
        assertThat(countChildRows()).isEqualTo(2 + 2 + 2); // categories + keywords + targets

        mvc.perform(delete("/api/alerts/" + id)).andExpect(status().isNoContent());

        assertThat(countChildRows()).isZero();
    }

    @Test
    void usersOnlySeeTheirOwnRules() throws Exception {
        mvc.perform(post("/api/users/2/alerts").contentType(MediaType.APPLICATION_JSON).content(BOB_RULE))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/users/2/alerts")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/users/1/alerts")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void adminSeesEveryUsersRulesOrderedByUser() throws Exception {
        mvc.perform(post("/api/users/2/alerts").contentType(MediaType.APPLICATION_JSON).content(BOB_RULE))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/users/1/alerts").contentType(MediaType.APPLICATION_JSON).content(BOB_RULE))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/admin/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", contains(1, 2)))
                .andExpect(jsonPath("$[0].categories", contains("NEWS", "MARKET")));
    }

    @Test
    void invalidTargetIsRejectedAndNothingIsStored() throws Exception {
        String badSlackUrl = BOB_RULE.replace("http://slack-mock:8080/services/bob", "slack-mock/bob");

        mvc.perform(post("/api/users/2/alerts").contentType(MediaType.APPLICATION_JSON).content(badSlackUrl))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("SLACK target must be an http(s) webhook URL: slack-mock/bob"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alert_rule", Integer.class)).isZero();
    }

    @Test
    void ruleLimitPerUserIsEnforcedWithTheShippedDefault() throws Exception {
        for (int i = 1; i <= 50; i++) {
            mvc.perform(post("/api/users/2/alerts").contentType(MediaType.APPLICATION_JSON).content(BOB_RULE))
                    .andExpect(status().isCreated());
        }

        mvc.perform(post("/api/users/2/alerts").contentType(MediaType.APPLICATION_JSON).content(BOB_RULE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("User 2 already has the maximum of 50 alert rules"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alert_rule WHERE user_id = 2", Integer.class))
                .isEqualTo(50);
        // The limit is per user: others are not affected.
        mvc.perform(post("/api/users/1/alerts").contentType(MediaType.APPLICATION_JSON).content(BOB_RULE))
                .andExpect(status().isCreated());
    }

    @Test
    void unknownUserIs404() throws Exception {
        mvc.perform(get("/api/users/999/alerts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("User 999 not found"));
        mvc.perform(post("/api/users/999/alerts").contentType(MediaType.APPLICATION_JSON).content(BOB_RULE))
                .andExpect(status().isNotFound());
    }

    @Test
    void publishesOpenApiDocument() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("World Event Alerts API"))
                .andExpect(jsonPath("$.paths['/api/users/{userId}/alerts'].post").exists())
                .andExpect(jsonPath("$.paths['/api/alerts/{id}'].put").exists());
    }

    private int countChildRows() {
        return jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM alert_rule_category)
                     + (SELECT COUNT(*) FROM alert_rule_keyword)
                     + (SELECT COUNT(*) FROM alert_rule_target)
                """, Integer.class);
    }
}
