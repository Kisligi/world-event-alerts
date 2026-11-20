package io.github.kisligi.alerts.event;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Limit;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private WorldEventRepository events;

    @MockitoBean
    private EventIngestionService ingestion;

    @Test
    void publishPassesAnAdminEventToIngestionAndReturnsMatches() throws Exception {
        Instant now = Instant.parse("2026-11-20T13:00:00Z");
        EventResponse stored = new EventResponse(5L, "ADMIN", null, EventCategory.DISASTER, Severity.CRITICAL,
                "M 7.2 earthquake", null, null, now, now);
        when(ingestion.ingest(any())).thenReturn(new IngestionResult(stored, false, List.of(3L, 4L)));

        mvc.perform(post("/api/admin/events").contentType(MediaType.APPLICATION_JSON).content("""
                        {"category": "DISASTER", "severity": "CRITICAL", "title": "  M 7.2 earthquake  "}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.event.id").value(5))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.matchedAlertRuleIds", contains(3, 4)));

        verify(ingestion).ingest(new IncomingEvent("ADMIN", null, EventCategory.DISASTER, Severity.CRITICAL,
                "M 7.2 earthquake", null, null, null));
    }

    @Test
    void publishRejectsIncompleteEvent() throws Exception {
        mvc.perform(post("/api/admin/events").contentType(MediaType.APPLICATION_JSON).content("""
                        {"category": null, "severity": "LOW", "title": " "}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", contains("category: must not be null", "title: must not be blank")));
        verifyNoInteractions(ingestion);
    }

    @Test
    void recentUsesDefaultLimitOf50() throws Exception {
        when(events.findAllByOrderByReceivedAtDescIdDesc(Limit.of(50))).thenReturn(List.of());

        mvc.perform(get("/api/events")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        verify(events).findAllByOrderByReceivedAtDescIdDesc(Limit.of(50));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 201})
    void recentRejectsLimitOutsideOneTo200(int limit) throws Exception {
        mvc.perform(get("/api/events").param("limit", String.valueOf(limit)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", startsWith("application/problem+json")));
        verifyNoInteractions(events);
    }
}
