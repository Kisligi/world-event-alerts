package io.github.kisligi.alerts.event.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.event.IncomingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Tests the USGS adapter against a fixture built from two real features of the live feed
 * (docs/planning/06-usgs-feed-check.md). No network access.
 */
class UsgsEarthquakeSourceTest {

    private static final URI FEED = URI.create("https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.geojson");

    private MockRestServiceServer server;
    private UsgsEarthquakeSource source;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        source = new UsgsEarthquakeSource(builder, FEED, Duration.ofMinutes(5));
    }

    @Test
    void mapsRealFeedFeaturesToDisasterEvents() {
        server.expect(requestTo(FEED)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ClassPathResource("usgs/4.5_week-sample.geojson"), MediaType.APPLICATION_JSON));

        List<IncomingEvent> events = source.poll();

        server.verify();
        assertThat(events).hasSize(2);
        IncomingEvent first = events.getFirst();
        assertThat(first.source()).isEqualTo("USGS");
        assertThat(first.externalId()).isEqualTo("us6000uc6c");
        assertThat(first.category()).isEqualTo(EventCategory.DISASTER);
        assertThat(first.severity()).isEqualTo(Severity.LOW);
        assertThat(first.title()).isEqualTo("M 4.9 - 54 km W of Madang, Papua New Guinea");
        assertThat(first.description()).isEqualTo("Magnitude 4.9 earthquake, 54 km W of Madang, Papua New Guinea.");
        assertThat(first.url()).isEqualTo("https://earthquake.usgs.gov/earthquakes/eventpage/us6000uc6c");
        assertThat(first.occurredAt()).isEqualTo(Instant.ofEpochMilli(1795151865106L));
        assertThat(events.get(1).externalId()).isEqualTo("us6000uc5s");
    }

    @Test
    void buildsTitleWhenMissingAndFlagsTsunami() {
        // "title" is in live data but not in the documented schema, so the adapter must cope without it.
        respondWith("""
                {"features": [{"id": "x1", "properties": {"mag": 7.1, "place": "off the coast", "tsunami": 1}}]}
                """);

        IncomingEvent event = source.poll().getFirst();

        assertThat(event.title()).isEqualTo("M 7.1 - off the coast");
        assertThat(event.description()).endsWith("Tsunami flag set by USGS.");
        assertThat(event.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(event.occurredAt()).isNull();
    }

    @Test
    void skipsFeaturesWithoutIdOrMagnitude() {
        respondWith("""
                {"features": [
                  {"id": null, "properties": {"mag": 5.0}},
                  {"id": "no-mag", "properties": {"mag": null, "place": "somewhere"}},
                  {"id": "no-props"},
                  {"id": "ok", "properties": {"mag": 5.0}}
                ]}
                """);

        assertThat(source.poll()).extracting(IncomingEvent::externalId).containsExactly("ok");
    }

    @ParameterizedTest(name = "body: {0}")
    @ValueSource(strings = {"{\"type\": \"FeatureCollection\", \"features\": []}", "{\"type\": \"FeatureCollection\"}"})
    void feedWithoutFeaturesGivesNoEvents(String body) {
        respondWith(body);

        assertThat(source.poll()).isEmpty();
    }

    @Test
    void emptyResponseBodyGivesNoEvents() {
        server.expect(requestTo(FEED)).andRespond(withSuccess());

        assertThat(source.poll()).isEmpty();
    }

    @Test
    void nameIsTheSourceStoredOnItsEvents() {
        // The poller logs by name() and duplicates are detected by the stored source, so they must agree.
        respondWith("{\"features\": [{\"id\": \"a\", \"properties\": {\"mag\": 5.5}}]}");

        assertThat(source.poll().getFirst().source()).isEqualTo(source.name());
        assertThat(source.pollInterval()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void serverErrorIsThrownSoThePollerCanLogAndRetry() {
        server.expect(requestTo(FEED)).andRespond(withServerError());

        assertThatThrownBy(() -> source.poll()).isInstanceOf(RestClientException.class);
    }

    @ParameterizedTest(name = "M {0} -> {1}")
    @CsvSource({"4.5, LOW", "4.99, LOW", "5.0, MEDIUM", "5.9, MEDIUM", "6.0, HIGH", "6.99, HIGH", "7.0, CRITICAL", "9.1, CRITICAL"})
    void severityFollowsMagnitudeScale(double magnitude, Severity expected) {
        assertThat(UsgsEarthquakeSource.severityFor(magnitude)).isEqualTo(expected);
    }

    private void respondWith(String json) {
        server.expect(requestTo(FEED)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
}
