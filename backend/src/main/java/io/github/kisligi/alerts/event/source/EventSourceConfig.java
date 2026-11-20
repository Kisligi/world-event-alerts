package io.github.kisligi.alerts.event.source;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

/** Creates the enabled event sources. Each one is switched on or off by configuration. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EventSourceProperties.class)
@EnableScheduling
public class EventSourceConfig {

    @Bean
    @ConditionalOnProperty(name = "alerts.sources.simulator.enabled", havingValue = "true", matchIfMissing = true)
    SimulatedEventSource simulatedEventSource(EventSourceProperties properties) {
        return new SimulatedEventSource(properties.simulator().interval());
    }

    @Bean
    @ConditionalOnProperty(name = "alerts.sources.usgs.enabled", havingValue = "true")
    UsgsEarthquakeSource usgsEarthquakeSource(RestClient.Builder builder, EventSourceProperties properties) {
        return new UsgsEarthquakeSource(builder, properties.usgs().feedUrl(), properties.usgs().interval());
    }
}
