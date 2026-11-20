package io.github.kisligi.alerts.event;

import java.time.Instant;

import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;

/**
 * An event as delivered by a source, before it is stored.
 *
 * @param source      who produced it, e.g. {@code SIMULATOR}, {@code USGS}, {@code ADMIN}
 * @param externalId  the source's own id, used to store each event only once; {@code null} if none
 * @param occurredAt  when it happened; {@code null} means "now"
 */
public record IncomingEvent(
        String source,
        String externalId,
        EventCategory category,
        Severity severity,
        String title,
        String description,
        String url,
        Instant occurredAt) {
}
