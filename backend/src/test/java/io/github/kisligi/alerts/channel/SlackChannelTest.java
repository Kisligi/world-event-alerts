package io.github.kisligi.alerts.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
}
