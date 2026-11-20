package io.github.kisligi.alerts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import com.jayway.jsonpath.JsonPath;
import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.event.EventIngestionService;
import io.github.kisligi.alerts.event.IncomingEvent;
import io.github.kisligi.alerts.event.IngestionResult;
import io.github.kisligi.alerts.event.WorldEventMatched;
import io.github.kisligi.alerts.event.source.SimulatedEventSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Events through the real stack: admin API, ingestion, matching against rules stored in
 * PostgreSQL, duplicate protection and the {@link WorldEventMatched} announcement.
 * Background polling is off so only the events created here exist.
 */
@SpringBootTest(properties = "alerts.sources.polling-enabled=false")
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(TestcontainersConfiguration.class)
class EventIngestionIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EventIngestionService ingestion;

    @Autowired
    private ApplicationEvents applicationEvents;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM alert_rule");
        jdbc.update("DELETE FROM world_event");
    }

    @Test
    void adminEventMatchesExactlyTheFittingRules() throws Exception {
        // Only the first two fit; each of the others fails on exactly one criterion.
        int quakes = createRule(1, "[\"DISASTER\"]", "[\"earthquake\"]", "HIGH", true);
        int anyDisaster = createRule(2, "[\"DISASTER\", \"NEWS\"]", "[]", "MEDIUM", true);
        createRule(1, "[\"DISASTER\"]", "[]", "CRITICAL", true);
        createRule(3, "[\"DISASTER\"]", "[\"flood\"]", "LOW", true);
        createRule(3, "[\"MARKET\"]", "[]", "LOW", true);
        createRule(2, "[\"DISASTER\"]", "[]", "LOW", false);

        mvc.perform(post("/api/admin/events").contentType(MediaType.APPLICATION_JSON).content("""
                        {"category": "DISASTER", "severity": "HIGH", "title": "Strong quake",
                         "description": "A magnitude 6.5 EARTHQUAKE was recorded."}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.event.source").value("ADMIN"))
                .andExpect(jsonPath("$.matchedAlertRuleIds", contains(quakes, anyDisaster)));

        assertThat(applicationEvents.stream(WorldEventMatched.class))
                .singleElement()
                .satisfies(e -> assertThat(e.alertRuleIds()).containsExactly((long) quakes, (long) anyDisaster));
    }

    @Test
    void sameEventFromSameSourceIsStoredOnce() {
        IncomingEvent quake = new IncomingEvent("USGS", "us6000uc6c", EventCategory.DISASTER, Severity.LOW,
                "M 4.9 - 54 km W of Madang, Papua New Guinea", null, null, null);

        IngestionResult first = ingestion.ingest(quake);
        IngestionResult second = ingestion.ingest(quake);

        assertThat(first.duplicate()).isFalse();
        assertThat(second.duplicate()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM world_event", Integer.class)).isEqualTo(1);
    }

    @Test
    void databaseAlsoRejectsDuplicateExternalIds() {
        // Safety net if two pollers ever race past the existence check.
        jdbc.update("""
                INSERT INTO world_event (source, external_id, category, severity, title, occurred_at, received_at)
                VALUES ('USGS', 'dup', 'DISASTER', 'LOW', 't', now(), now())""");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO world_event (source, external_id, category, severity, title, occurred_at, received_at)
                VALUES ('USGS', 'dup', 'DISASTER', 'LOW', 't', now(), now())"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void simulatorEventsFitTheDatabaseAndAreListedNewestFirst() throws Exception {
        SimulatedEventSource simulator = new SimulatedEventSource(Duration.ofSeconds(30));
        List<String> titles = IntStream.range(0, 3)
                .mapToObj(i -> ingestion.ingest(simulator.poll().getFirst()).event().title())
                .toList();

        String body = mvc.perform(get("/api/events").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].source").value("SIMULATOR"))
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<List<String>>read(body, "$[*].title")).containsExactly(titles.get(2), titles.get(1));
    }

    private int createRule(int userId, String categories, String keywords, String minSeverity, boolean enabled)
            throws Exception {
        String response = mvc.perform(post("/api/users/" + userId + "/alerts")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": "rule", "categories": %s, "keywords": %s, "minSeverity": "%s",
                                 "targets": [{"type": "EMAIL", "address": "test@example.com"}], "enabled": %s}
                                """.formatted(categories, keywords, minSeverity, enabled)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
