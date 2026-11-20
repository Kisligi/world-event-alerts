package io.github.kisligi.alerts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import io.github.kisligi.alerts.TestcontainersConfiguration.MailpitContainer;
import io.github.kisligi.alerts.TestcontainersConfiguration.WireMockContainer;
import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.event.EventIngestionService;
import io.github.kisligi.alerts.event.IncomingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

/**
 * The whole chain with real infrastructure: an event is ingested, matched, and delivered over
 * real SMTP to Mailpit and real HTTP to the WireMock Slack stand-in (same images and mappings
 * as docker compose). Retries are made fast; everything else is production configuration.
 */
@SpringBootTest(properties = {
        "alerts.sources.polling-enabled=false",
        "alerts.delivery.initial-backoff=10ms"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DeliveryIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EventIngestionService ingestion;

    @Autowired
    private MailpitContainer mailpit;

    @Autowired
    private WireMockContainer wireMock;

    private final RestClient http = RestClient.create();

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM delivery");
        jdbc.update("DELETE FROM alert_rule");
        jdbc.update("DELETE FROM world_event");
        jdbc.update("UPDATE channel_setting SET enabled = TRUE");
        http.delete().uri(mailpit.apiUrl() + "/api/v1/messages").retrieve().toBodilessEntity();
        http.delete().uri(wireMock.baseUrl() + "/__admin/requests").retrieve().toBodilessEntity();
    }

    @Test
    void matchedEventIsDeliveredByEmailAndSlackAndLogged() throws Exception {
        createRule(1, "Big quakes", slackUrl("/services/alice"));

        publishQuake("M 7.2 earthquake near Tokyo");

        // Email arrived in Mailpit with our subject, sender and recipient.
        String inbox = http.get().uri(mailpit.apiUrl() + "/api/v1/messages").retrieve().body(String.class);
        assertThat(JsonPath.<Integer>read(inbox, "$.total")).isEqualTo(1);
        assertThat(JsonPath.<String>read(inbox, "$.messages[0].Subject"))
                .isEqualTo("[CRITICAL] Disaster: M 7.2 earthquake near Tokyo");
        assertThat(JsonPath.<String>read(inbox, "$.messages[0].To[0].Address")).isEqualTo("alice@example.com");
        assertThat(JsonPath.<String>read(inbox, "$.messages[0].From.Address")).isEqualTo("alerts@example.com");

        // Slack stand-in received the webhook JSON.
        List<String> slackBodies = slackRequestBodies("/services/alice");
        assertThat(slackBodies).singleElement().satisfies(body ->
                assertThat(JsonPath.<String>read(body, "$.text"))
                        .startsWith("*[CRITICAL] Disaster: M 7.2 earthquake near Tokyo*\n"));

        // Both deliveries are in Alice's history as SENT after one attempt.
        String history = mvc.perform(get("/api/users/1/notifications"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(history, "$[*].status")).containsExactly("SENT", "SENT");
        assertThat(JsonPath.<List<Integer>>read(history, "$[*].attempts")).containsExactly(1, 1);
        assertThat(JsonPath.<List<String>>read(history, "$[*].alertRuleName")).containsOnly("Big quakes");
    }

    @Test
    void repeatedSourceEventDoesNotNotifyTwice() throws Exception {
        createRule(1, "Big quakes", slackUrl("/services/alice"));
        IncomingEvent quake = new IncomingEvent("USGS", "us7000test", EventCategory.DISASTER, Severity.CRITICAL,
                "M 7.0 earthquake", null, null, null);

        ingestion.ingest(quake);
        ingestion.ingest(quake);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM delivery", Integer.class)).isEqualTo(2);
        assertThat(slackRequestBodies("/services/alice")).hasSize(1);
    }

    @Test
    void brokenWebhookIsRetriedThenLoggedAsFailedWhileEmailStillArrives() throws Exception {
        createRule(1, "Big quakes", slackUrl("/services/fail/alice"));

        publishQuake("Quake with Slack outage");

        assertThat(slackRequestBodies("/services/fail/alice")).hasSize(3);
        String failed = mvc.perform(get("/api/admin/deliveries").param("status", "FAILED"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(failed, "$[*].channelType")).containsExactly("SLACK");
        assertThat(JsonPath.<List<Integer>>read(failed, "$[*].attempts")).containsExactly(3);
        assertThat(JsonPath.<String>read(failed, "$[0].lastError")).contains("500");
        assertThat(JsonPath.<String>read(failed, "$[0].lastError")).contains("simulated Slack outage");
        assertThat(mailCount()).isEqualTo(1);
    }

    @Test
    void channelSwitchedOffByAdminIsSkipped() throws Exception {
        createRule(1, "Big quakes", slackUrl("/services/alice"));
        mvc.perform(put("/api/admin/channels/SLACK").contentType(MediaType.APPLICATION_JSON).content("{\"enabled\": false}"))
                .andExpect(status().isOk());

        publishQuake("Quake while Slack is off");

        assertThat(slackRequestBodies("/services/alice")).isEmpty();
        String skipped = mvc.perform(get("/api/admin/deliveries").param("status", "SKIPPED"))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(skipped, "$[*].channelType")).containsExactly("SLACK");
        assertThat(mailCount()).isEqualTo(1);
    }

    @Test
    void deletingTheRuleKeepsItsDeliveryHistory() throws Exception {
        int ruleId = createRule(1, "Big quakes", slackUrl("/services/alice"));
        publishQuake("Quake before deleting the rule");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/alerts/" + ruleId))
                .andExpect(status().isNoContent());

        String history = mvc.perform(get("/api/users/1/notifications"))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(history, "$[*].alertRuleName")).containsExactly("Big quakes", "Big quakes");
        assertThat(JsonPath.<List<Object>>read(history, "$[*].alertRuleId")).containsOnlyNulls();
    }

    private String slackUrl(String path) {
        return wireMock.baseUrl() + path;
    }

    private void publishQuake(String title) throws Exception {
        mvc.perform(post("/api/admin/events").contentType(MediaType.APPLICATION_JSON).content("""
                        {"category": "DISASTER", "severity": "CRITICAL", "title": "%s"}
                        """.formatted(title)))
                .andExpect(status().isCreated());
    }

    private int createRule(int userId, String name, String slackWebhook) throws Exception {
        String body = mvc.perform(post("/api/users/" + userId + "/alerts").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "categories": ["DISASTER"], "minSeverity": "HIGH",
                                 "targets": [{"type": "EMAIL", "address": "alice@example.com"},
                                             {"type": "SLACK", "address": "%s"}]}
                                """.formatted(name, slackWebhook)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private int mailCount() {
        String inbox = http.get().uri(mailpit.apiUrl() + "/api/v1/messages").retrieve().body(String.class);
        return JsonPath.read(inbox, "$.total");
    }

    /** Bodies of the POST requests WireMock received on the given path. */
    private List<String> slackRequestBodies(String path) {
        String found = http.post().uri(wireMock.baseUrl() + "/__admin/requests/find")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("method", "POST", "url", path))
                .retrieve().body(String.class);
        return JsonPath.read(found, "$.requests[*].body");
    }
}
