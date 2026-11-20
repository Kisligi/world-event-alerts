package io.github.kisligi.alerts.event.source;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings under {@code alerts.sources}. Defaults: the simulator is on, USGS is off, so the
 * default run works offline (docs/decisions.md ADR-003).
 */
@ConfigurationProperties("alerts.sources")
public record EventSourceProperties(
        @DefaultValue("true") boolean pollingEnabled,
        @DefaultValue Simulator simulator,
        @DefaultValue Usgs usgs) {

    public record Simulator(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("30s") Duration interval) {
    }

    public record Usgs(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("5m") Duration interval,
            @DefaultValue("https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_day.geojson")
            URI feedUrl) {
    }
}
