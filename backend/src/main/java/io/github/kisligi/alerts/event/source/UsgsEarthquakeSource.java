package io.github.kisligi.alerts.event.source;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.event.IncomingEvent;
import org.springframework.web.client.RestClient;

/**
 * Real earthquakes from the public USGS GeoJSON summary feed (no API key needed).
 * Off by default; enable with {@code alerts.sources.usgs.enabled=true}.
 *
 * <p>Format verified against the USGS documentation and live data: see
 * docs/planning/06-usgs-feed-check.md.
 */
public class UsgsEarthquakeSource implements EventSource {

    public static final String NAME = "USGS";

    private final RestClient client;
    private final URI feedUrl;
    private final Duration interval;

    public UsgsEarthquakeSource(RestClient.Builder builder, URI feedUrl, Duration interval) {
        this.client = builder.build();
        this.feedUrl = feedUrl;
        this.interval = interval;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Duration pollInterval() {
        return interval;
    }

    @Override
    public List<IncomingEvent> poll() {
        FeatureCollection feed = client.get().uri(feedUrl).retrieve().body(FeatureCollection.class);
        if (feed == null || feed.features() == null) {
            return List.of();
        }
        return feed.features().stream()
                .filter(f -> f.id() != null && f.properties() != null && f.properties().mag() != null)
                .map(UsgsEarthquakeSource::toIncomingEvent)
                .toList();
    }

    private static IncomingEvent toIncomingEvent(Feature feature) {
        Properties p = feature.properties();
        String place = p.place() == null ? "unknown location" : p.place();
        String title = p.title() != null ? p.title() : "M " + p.mag() + " - " + place;
        String description = "Magnitude " + p.mag() + " earthquake, " + place + "."
                + (p.tsunami() != null && p.tsunami() == 1 ? " Tsunami flag set by USGS." : "");
        Instant occurredAt = p.time() == null ? null : Instant.ofEpochMilli(p.time());
        return new IncomingEvent(NAME, feature.id(), EventCategory.DISASTER, severityFor(p.mag()),
                title, description, p.url(), occurredAt);
    }

    /**
     * Maps magnitude to severity. Our own simple scale, not a USGS classification:
     * below 5 LOW, 5 to below 6 MEDIUM, 6 to below 7 HIGH, 7 and above CRITICAL.
     */
    static Severity severityFor(double magnitude) {
        if (magnitude >= 7.0) {
            return Severity.CRITICAL;
        }
        if (magnitude >= 6.0) {
            return Severity.HIGH;
        }
        if (magnitude >= 5.0) {
            return Severity.MEDIUM;
        }
        return Severity.LOW;
    }

    // Only the fields we use; everything else in the feed is ignored.

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FeatureCollection(List<Feature> features) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Feature(String id, Properties properties) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Properties(Double mag, String place, Long time, String url, String title, Integer tsunami) {
    }
}
