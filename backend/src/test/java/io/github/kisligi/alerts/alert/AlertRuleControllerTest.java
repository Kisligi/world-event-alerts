package io.github.kisligi.alerts.alert;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import io.github.kisligi.alerts.common.ChannelType;
import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.NotFoundException;
import io.github.kisligi.alerts.common.Severity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web-layer tests: request validation, status codes and error format. The service is mocked. */
@WebMvcTest(AlertRuleController.class)
class AlertRuleControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AlertRuleService service;

    private static final String VALID_BODY = """
            {
              "name": "Major earthquakes",
              "categories": ["DISASTER"],
              "keywords": ["earthquake"],
              "minSeverity": "HIGH",
              "targets": [{"type": "EMAIL", "address": "alice@example.com"}]
            }
            """;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.create(eq(1L), any())).thenReturn(sampleResponse(10L));

        mvc.perform(post("/api/users/1/alerts").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/alerts/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.categories", contains("DISASTER")));
    }

    @Test
    void createWithMissingFieldsReturns400ProblemDetail() throws Exception {
        String body = """
                {"name": "", "categories": [], "minSeverity": null, "targets": []}
                """;

        mvc.perform(post("/api/users/1/alerts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(4));
        verifyNoInteractions(service);
    }

    @Test
    void createWithUnknownEnumValueReturns400() throws Exception {
        String body = VALID_BODY.replace("DISASTER", "SPORTS");

        mvc.perform(post("/api/users/1/alerts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void getMissingRuleReturns404ProblemDetail() throws Exception {
        when(service.get(42L)).thenThrow(new NotFoundException("Alert rule", 42L));

        mvc.perform(get("/api/alerts/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Alert rule 42 not found"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/alerts/3")).andExpect(status().isNoContent());
    }

    private static AlertRuleResponse sampleResponse(Long id) {
        Instant now = Instant.parse("2026-11-20T12:00:00Z");
        return new AlertRuleResponse(id, 1L, "Major earthquakes", List.of(EventCategory.DISASTER),
                List.of("earthquake"), Severity.HIGH,
                List.of(new ChannelTarget(ChannelType.EMAIL, "alice@example.com")), true, now, now);
    }
}
