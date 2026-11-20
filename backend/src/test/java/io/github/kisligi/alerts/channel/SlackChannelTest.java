package io.github.kisligi.alerts.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

import io.github.kisligi.alerts.common.ChannelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SlackChannelTest {

    private static final String WEBHOOK = "https://hooks.slack.com/services/T000/B000/XXXX";

    private MockRestServiceServer server;
    private SlackChannel channel;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        channel = new SlackChannel(builder);
    }

    @Test
    void postsSlackWebhookJsonWithBoldSubject() {
        server.expect(requestTo(WEBHOOK))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"text\": \"*[HIGH] Disaster: Quake*\\nDetails\"}", true))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        channel.send(WEBHOOK, new Notification("[HIGH] Disaster: Quake", "Details"));

        server.verify();
        assertThat(channel.type()).isEqualTo(ChannelType.SLACK);
    }

    @Test
    void errorResponseBecomesDeliveryExceptionSoTheDispatcherRetries() {
        server.expect(requestTo(WEBHOOK)).andRespond(withServerError());

        assertThatThrownBy(() -> channel.send(WEBHOOK, new Notification("s", "b")))
                .isInstanceOf(DeliveryException.class)
                .hasMessageStartingWith("Slack webhook call failed");
    }

    @Test
    void unreachableWebhookNamesTheConnectionErrorInsteadOfNull() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        String url = "http://localhost:" + closedPort + "/services/x";
        SlackChannel realClient = new SlackChannel(RestClient.builder());

        assertThatThrownBy(() -> realClient.send(url, new Notification("s", "b")))
                .isInstanceOf(DeliveryException.class)
                .hasMessage("Slack webhook call failed: ConnectException calling " + url);
    }

    @Test
    void timeoutKeepsItsOwnMessage() {
        server.expect(requestTo(WEBHOOK)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> channel.send(WEBHOOK, new Notification("s", "b")))
                .isInstanceOf(DeliveryException.class)
                .hasMessageStartingWith("Slack webhook call failed: I/O error on POST request")
                .hasMessageEndingWith("Read timed out");
    }
}
