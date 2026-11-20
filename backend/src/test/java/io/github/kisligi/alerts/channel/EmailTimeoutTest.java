package io.github.kisligi.alerts.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * A mail server that accepts the connection but never answers must not block delivery forever.
 * Jakarta Mail waits indefinitely by default; a stuck send would hold the polling thread (and
 * stop every event source) or a request thread (review-log R7).
 */
class EmailTimeoutTest {

    /** Loads the real application.yaml, so these tests check the shipped configuration. */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
            .withUserConfiguration(EmailChannel.class);

    private ServerSocket silentServer;
    private final List<Socket> accepted = new ArrayList<>();

    @BeforeEach
    void startSilentServer() throws IOException {
        silentServer = new ServerSocket(0);
        Thread acceptor = new Thread(() -> {
            try {
                while (true) {
                    accepted.add(silentServer.accept()); // keep it open, never write a greeting
                }
            } catch (IOException closed) {
                // server stopped
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
    }

    @AfterEach
    void stopSilentServer() throws IOException {
        silentServer.close();
        for (Socket socket : accepted) {
            socket.close();
        }
    }

    @Test
    void applicationConfigurationSetsSmtpTimeouts() {
        runner.run(context -> {
            JavaMailSenderImpl sender = context.getBean(JavaMailSenderImpl.class);
            assertThat(sender.getJavaMailProperties())
                    .containsEntry("mail.smtp.connectiontimeout", "5000")
                    .containsEntry("mail.smtp.timeout", "10000")
                    .containsEntry("mail.smtp.writetimeout", "10000");
        });
    }

    @Test
    void sendToAServerThatNeverAnswersFailsInsteadOfHanging() {
        // Same property as in application.yaml, shortened so the test is quick.
        runner.withPropertyValues(
                        "spring.mail.host=localhost",
                        "spring.mail.port=" + silentServer.getLocalPort(),
                        "spring.mail.properties[mail.smtp.timeout]=500")
                .run(context -> {
                    EmailChannel channel = context.getBean(EmailChannel.class);
                    assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                            assertThatThrownBy(() -> channel.send("alice@example.com",
                                    new Notification("subject", "body")))
                                    .isInstanceOf(DeliveryException.class)
                                    .hasMessageContaining("SMTP send failed"));
                });
    }
}
