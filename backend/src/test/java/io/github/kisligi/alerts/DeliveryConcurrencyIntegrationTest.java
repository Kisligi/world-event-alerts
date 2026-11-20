package io.github.kisligi.alerts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.github.kisligi.alerts.TestcontainersConfiguration.WireMockContainer;
import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.event.EventIngestionService;
import io.github.kisligi.alerts.event.IncomingEvent;
import io.github.kisligi.alerts.event.IngestionResult;
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
 * Several events delivered at the same time must not exhaust the database connection pool.
 *
 * <p>Regression test for a bug found in the M3 review (docs/review-log.md R7): delivery ran while
 * the ingesting thread still held its database connection, and every delivery step borrowed a
 * second one. With as many concurrent deliveries as pooled connections, every thread waited for
 * a connection held by another, and notifications were lost. Here the pool has only 2
 * connections, each Slack call takes 1 s, and 4 events are ingested in parallel.
 */
@SpringBootTest(properties = {
        "alerts.sources.polling-enabled=false",
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.connection-timeout=1000"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DeliveryConcurrencyIntegrationTest {

    private static final int EVENTS = 4;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EventIngestionService ingestion;

    @Autowired
    private WireMockContainer wireMock;

    @Test
    void concurrentEventsAreAllDeliveredWithASmallConnectionPool() throws Exception {
        // A Slack stand-in that answers after 1 s, so deliveries overlap.
        RestClient.create().post().uri(wireMock.baseUrl() + "/__admin/mappings")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"request": {"method": "POST", "urlPath": "/services/slow/demo"},
                         "response": {"status": 200, "body": "ok", "fixedDelayMilliseconds": 1000}}
                        """)
                .retrieve().toBodilessEntity();
        mvc.perform(post("/api/users/1/alerts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Every disaster", "categories": ["DISASTER"], "minSeverity": "LOW",
                         "targets": [{"type": "SLACK", "address": "%s/services/slow/demo"}]}
                        """.formatted(wireMock.baseUrl())))
                .andExpect(status().isCreated());

        ExecutorService pool = Executors.newFixedThreadPool(EVENTS);
        List<Future<IngestionResult>> results = new ArrayList<>();
        for (int i = 1; i <= EVENTS; i++) {
            IncomingEvent event = new IncomingEvent("TEST", "concurrent-" + i, EventCategory.DISASTER,
                    Severity.HIGH, "Concurrent event " + i, null, null, null);
            results.add(pool.submit(() -> ingestion.ingest(event)));
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // Every ingestion succeeded (get() rethrows a failure) ...
        for (Future<IngestionResult> result : results) {
            assertThat(result.get().matchedAlertRuleIds()).hasSize(1);
        }
        // ... and every event produced exactly one successful delivery, sent on the first try.
        assertThat(jdbc.queryForList("SELECT status FROM delivery", String.class))
                .hasSize(EVENTS).containsOnly("SENT");
        assertThat(jdbc.queryForList("SELECT attempts FROM delivery", Integer.class)).containsOnly(1);
    }
}
