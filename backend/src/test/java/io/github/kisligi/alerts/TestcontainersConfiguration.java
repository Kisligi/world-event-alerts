package io.github.kisligi.alerts;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Starts the app's infrastructure in Docker, using the same images and WireMock mappings as
 * {@code compose.yaml}: PostgreSQL, Mailpit (SMTP + inbox API) and WireMock (Slack stand-in).
 * Used by integration tests and by {@link TestWorldEventAlertsApplication}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final String POSTGRES_IMAGE = "postgres:17-alpine";
    static final String MAILPIT_IMAGE = "axllent/mailpit:v1.34.0";
    static final String WIREMOCK_IMAGE = "wiremock/wiremock:3.13.2";

    /** Mailpit: SMTP on 1025, web UI and REST API on 8025. */
    public static class MailpitContainer extends GenericContainer<MailpitContainer> {
        MailpitContainer() {
            super(MAILPIT_IMAGE);
            withExposedPorts(1025, 8025);
            waitingFor(Wait.forHttp("/api/v1/messages").forPort(8025));
        }

        public String apiUrl() {
            return "http://" + getHost() + ":" + getMappedPort(8025);
        }
    }

    /** WireMock with the Slack webhook mappings from infra/wiremock (shared with compose). */
    public static class WireMockContainer extends GenericContainer<WireMockContainer> {
        WireMockContainer() {
            super(WIREMOCK_IMAGE);
            withExposedPorts(8080);
            withCopyFileToContainer(MountableFile.forHostPath("../infra/wiremock/mappings/slack-webhook.json"),
                    "/home/wiremock/mappings/slack-webhook.json");
            waitingFor(Wait.forHttp("/__admin/mappings").forPort(8080));
        }

        public String baseUrl() {
            return "http://" + getHost() + ":" + getMappedPort(8080);
        }
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }

    @Bean
    MailpitContainer mailpitContainer() {
        return new MailpitContainer();
    }

    @Bean
    WireMockContainer wireMockContainer() {
        return new WireMockContainer();
    }

    @Bean
    DynamicPropertyRegistrar mailProperties(MailpitContainer mailpit) {
        return registry -> {
            registry.add("spring.mail.host", mailpit::getHost);
            registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
        };
    }
}
