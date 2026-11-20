package io.github.kisligi.alerts.alert;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.github.kisligi.alerts.common.ChannelType;
import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.InvalidRequestException;
import io.github.kisligi.alerts.common.NotFoundException;
import io.github.kisligi.alerts.common.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    void businessRuleViolationFromServiceReturns400ProblemDetail() throws Exception {
        // Bean Validation passes (type and address are present), but the service rejects
        // an address that does not fit the channel. The handler must turn that into a 400.
        when(service.create(eq(1L), any()))
                .thenThrow(new InvalidRequestException("SLACK target must be an http(s) webhook URL: nope"));
        String body = VALID_BODY.replace("\"EMAIL\", \"address\": \"alice@example.com\"",
                "\"SLACK\", \"address\": \"nope\"");

        mvc.perform(post("/api/users/1/alerts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", startsWith("application/problem+json")))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("SLACK target must be an http(s) webhook URL: nope"))
                .andExpect(jsonPath("$.instance").value("/api/users/1/alerts"));
    }

    /** Each case breaks exactly one constraint of {@link AlertRuleRequest}. */
    static Stream<Arguments> constraintViolations() {
        String keywords21 = IntStream.rangeClosed(1, 21).mapToObj(i -> "\"k" + i + "\"")
                .collect(Collectors.joining(","));
        String target = "{\"type\": \"EMAIL\", \"address\": \"alice@example.com\"}";
        String targets11 = Stream.generate(() -> target).limit(11).collect(Collectors.joining(","));
        return Stream.of(
                Arguments.of("name longer than 100", "\"name\": \"Major earthquakes\"",
                        "\"name\": \"" + "x".repeat(101) + "\"", "name"),
                Arguments.of("more than 20 keywords", "\"keywords\": [\"earthquake\"]",
                        "\"keywords\": [" + keywords21 + "]", "keywords"),
                Arguments.of("blank keyword", "\"keywords\": [\"earthquake\"]",
                        "\"keywords\": [\" \"]", "keywords"),
                Arguments.of("more than 10 targets", "\"targets\": [" + target + "]",
                        "\"targets\": [" + targets11 + "]", "targets"),
                Arguments.of("target without type", "\"type\": \"EMAIL\", ", "", "targets[0].type"),
                Arguments.of("target with blank address", "\"address\": \"alice@example.com\"",
                        "\"address\": \"\"", "targets[0].address"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("constraintViolations")
    void rejectsRequestThatBreaksOneConstraint(String description, String validPart, String invalidPart,
                                               String expectedField) throws Exception {
        String body = VALID_BODY.replace(validPart, invalidPart);

        mvc.perform(post("/api/users/1/alerts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors", hasItem(startsWith(expectedField))));
        verifyNoInteractions(service);
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/users/1/alerts").contentType(MediaType.APPLICATION_JSON).content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", startsWith("application/problem+json")));
        verifyNoInteractions(service);
    }

    @Test
    void adminListsEveryUsersRules() throws Exception {
        when(service.listAll()).thenReturn(List.of(sampleResponse(10L), sampleResponse(11L)));

        mvc.perform(get("/api/admin/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(10, 11)))
                .andExpect(jsonPath("$[0].userId").exists());
    }

    @Test
    void getExistingRuleReturnsIt() throws Exception {
        when(service.get(10L)).thenReturn(sampleResponse(10L));

        mvc.perform(get("/api/alerts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Major earthquakes"))
                .andExpect(jsonPath("$.targets[0].address").value("alice@example.com"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-20T12:00:00Z"));
    }

    @Test
    void updateValidatesBodyLikeCreate() throws Exception {
        mvc.perform(put("/api/alerts/10").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"HIGH\"", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", contains(startsWith("minSeverity"))));
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
