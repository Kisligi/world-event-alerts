package io.github.kisligi.alerts.event;

import java.time.Instant;

import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;

/** A stored world event as returned by the API. */
public record EventResponse(
        Long id,
        String source,
        String externalId,
        EventCategory category,
        Severity severity,
        String title,
        String description,
        String url,
        Instant occurredAt,
        Instant receivedAt) {

    static EventResponse from(WorldEvent event) {
        return new EventResponse(event.getId(), event.getSource(), event.getExternalId(),
                event.getCategory(), event.getSeverity(), event.getTitle(), event.getDescription(),
                event.getUrl(), event.getOccurredAt(), event.getReceivedAt());
    }
}
