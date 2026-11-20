package io.github.kisligi.alerts.event.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

/** Checks which sources exist for a given configuration (docs/decisions.md ADR-003). */
class EventSourceConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(EventSourceConfig.class)
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void defaultsAreSimulatorOnAndUsgsOff() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(SimulatedEventSource.class);
            assertThat(context).doesNotHaveBean(UsgsEarthquakeSource.class);
            assertThat(context.getBean(SimulatedEventSource.class).pollInterval()).isEqualTo(Duration.ofSeconds(15));
            assertThat(context.getBean(EventSourceProperties.class).usgs().feedUrl())
                    .isEqualTo(URI.create("https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_day.geojson"));
        });
    }

    @Test
    void usgsCanBeSwitchedOnAndSimulatorOff() {
        runner.withPropertyValues(
                        "alerts.sources.usgs.enabled=true",
                        "alerts.sources.usgs.interval=2m",
                        "alerts.sources.simulator.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SimulatedEventSource.class);
                    assertThat(context).hasSingleBean(UsgsEarthquakeSource.class);
                    assertThat(context.getBean(UsgsEarthquakeSource.class).pollInterval()).isEqualTo(Duration.ofMinutes(2));
                });
    }
}
