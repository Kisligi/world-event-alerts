package io.github.kisligi.alerts.delivery;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import io.github.kisligi.alerts.channel.Notification;
import io.github.kisligi.alerts.event.WorldEvent;
import org.springframework.stereotype.Component;

/** Turns a matched event into the channel-neutral notification text. */
@Component
public class NotificationFormatter {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'", Locale.ROOT).withZone(ZoneOffset.UTC);

    public Notification format(WorldEvent event, String alertRuleName) {
        String category = event.getCategory().name().charAt(0)
                + event.getCategory().name().substring(1).toLowerCase(Locale.ROOT);
        String subject = "[" + event.getSeverity() + "] " + category + ": " + event.getTitle();

        StringBuilder body = new StringBuilder(event.getTitle()).append("\n\n");
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            body.append(event.getDescription()).append("\n\n");
        }
        body.append("Category: ").append(category).append('\n')
                .append("Severity: ").append(event.getSeverity()).append('\n')
                .append("Source: ").append(event.getSource()).append('\n')
                .append("Occurred: ").append(TIME.format(event.getOccurredAt())).append('\n');
        if (event.getUrl() != null) {
            body.append("More: ").append(event.getUrl()).append('\n');
        }
        body.append("\nYou receive this because of your alert rule \"").append(alertRuleName).append("\".");
        return new Notification(subject, body.toString());
    }
}
