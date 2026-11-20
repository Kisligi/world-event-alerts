package io.github.kisligi.alerts.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import io.github.kisligi.alerts.channel.Notification;
import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.event.IncomingEvent;
import io.github.kisligi.alerts.event.WorldEvent;
import org.junit.jupiter.api.Test;

class NotificationFormatterTest {

    private final NotificationFormatter formatter = new NotificationFormatter();

    @Test
    void fullEventProducesSubjectWithSeverityAndCategoryAndAllDetails() {
        WorldEvent event = WorldEvent.from(new IncomingEvent("USGS", "us1", EventCategory.DISASTER, Severity.HIGH,
                "M 6.5 - 10 km N of Somewhere", "Magnitude 6.5 earthquake.",
                "https://earthquake.usgs.gov/earthquakes/eventpage/us1", Instant.parse("2026-11-20T09:05:59Z")));

        Notification n = formatter.format(event, "Big quakes");

        assertThat(n.subject()).isEqualTo("[HIGH] Disaster: M 6.5 - 10 km N of Somewhere");
        assertThat(n.body()).isEqualTo("""
                M 6.5 - 10 km N of Somewhere

                Magnitude 6.5 earthquake.

                Category: Disaster
                Severity: HIGH
                Source: USGS
                Occurred: 2026-11-20 09:05 UTC
                More: https://earthquake.usgs.gov/earthquakes/eventpage/us1

                You receive this because of your alert rule "Big quakes".""");
    }

    @Test
    void missingDescriptionAndUrlLeaveNoEmptyLines() {
        WorldEvent event = WorldEvent.from(new IncomingEvent("ADMIN", null, EventCategory.MARKET, Severity.CRITICAL,
                "Index falls 12%", "  ", null, Instant.parse("2026-11-20T13:00:00Z")));

        Notification n = formatter.format(event, "Crashes");

        assertThat(n.subject()).isEqualTo("[CRITICAL] Market: Index falls 12%");
        assertThat(n.body()).doesNotContain("More:").doesNotContain("\n\n\n");
        assertThat(n.body()).startsWith("Index falls 12%\n\nCategory: Market\n");
    }
}
