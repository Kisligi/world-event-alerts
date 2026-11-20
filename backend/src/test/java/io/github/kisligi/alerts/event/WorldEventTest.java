package io.github.kisligi.alerts.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import org.junit.jupiter.api.Test;

class WorldEventTest {

    @Test
    void occurredAtDefaultsToReceivedAtWhenSourceGivesNone() {
        WorldEvent event = WorldEvent.from(incoming(null, "Title", null));

        assertThat(event.getOccurredAt()).isEqualTo(event.getReceivedAt());
    }

    @Test
    void occurredAtFromSourceIsKeptAtDatabasePrecision() {
        // PostgreSQL stores microseconds; anything finer would change after a round trip.
        WorldEvent event = WorldEvent.from(incoming(Instant.parse("2026-11-20T10:15:30.123456789Z"), "Title", null));

        assertThat(event.getOccurredAt()).isEqualTo(Instant.parse("2026-11-20T10:15:30.123456Z"));
    }

    @Test
    void searchableTextIncludesDescriptionOnlyWhenPresent() {
        assertThat(WorldEvent.from(incoming(null, "Title", null)).searchableText()).isEqualTo("Title");
        assertThat(WorldEvent.from(incoming(null, "Title", "Details")).searchableText()).isEqualTo("Title\nDetails");
    }

    private static IncomingEvent incoming(Instant occurredAt, String title, String description) {
        return new IncomingEvent("TEST", "x", EventCategory.NEWS, Severity.LOW, title, description, null, occurredAt);
    }
}
