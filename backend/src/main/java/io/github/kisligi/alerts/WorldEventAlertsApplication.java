package io.github.kisligi.alerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the World Event Alerts service.
 *
 * <p>Users define alert rules; when a matching world event arrives, they are notified
 * through one or more channels (email, Slack, ...). See {@code docs/architecture.md}.
 */
@SpringBootApplication
public class WorldEventAlertsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorldEventAlertsApplication.class, args);
    }
}
