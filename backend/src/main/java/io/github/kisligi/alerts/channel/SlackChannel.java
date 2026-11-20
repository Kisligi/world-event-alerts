package io.github.kisligi.alerts.channel;

import java.util.Map;

import io.github.kisligi.alerts.common.ChannelType;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Posts notifications to a Slack incoming webhook: an HTTP POST with a JSON body
 * {@code {"text": "..."}}. In the demo setup the webhook URL points to a local WireMock
 * stand-in; a real {@code https://hooks.slack.com/services/...} URL works the same way.
 */
@Component
public class SlackChannel implements NotificationChannel {

    private final RestClient client;

    public SlackChannel(RestClient.Builder builder) {
        this.client = builder.build();
    }

    @Override
    public ChannelType type() {
        return ChannelType.SLACK;
    }

    @Override
    public void send(String webhookUrl, Notification notification) {
        // Slack "mrkdwn": *bold* subject line, then the details.
        String text = "*" + notification.subject() + "*\n" + notification.body();
        try {
            client.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new DeliveryException("Slack webhook call failed: " + e.getMessage(), e);
        }
    }
}
