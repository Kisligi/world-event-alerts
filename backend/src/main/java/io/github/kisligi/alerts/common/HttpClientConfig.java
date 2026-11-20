package io.github.kisligi.alerts.common;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/**
 * Settings for every outgoing HTTP call (USGS feed, Slack webhooks).
 *
 * <ul>
 *   <li><b>HTTP/1.1:</b> the JDK client otherwise tries to upgrade plain {@code http://}
 *       connections to HTTP/2, which the local Slack stand-in (WireMock) answered with
 *       "RST_STREAM: Stream cancelled", so every Slack delivery failed. Found by
 *       {@code DeliveryIntegrationTest}; see docs/dev-journal.md (M3).</li>
 *   <li><b>Timeouts:</b> a slow webhook must not block delivery forever.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class HttpClientConfig {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    RestClientCustomizer http11WithTimeouts() {
        return builder -> builder.requestFactory(requestFactory());
    }

    static JdkClientHttpRequestFactory requestFactory() {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }
}
