package io.github.kisligi.alerts;

import org.springframework.boot.SpringApplication;

/**
 * Runs the app locally against a Testcontainers PostgreSQL, without docker compose:
 * {@code ./mvnw spring-boot:test-run}. Needs Docker.
 */
public class TestWorldEventAlertsApplication {

    public static void main(String[] args) {
        SpringApplication.from(WorldEventAlertsApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
