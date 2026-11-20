package io.github.kisligi.alerts.event;

import java.util.List;

/**
 * Outcome of ingesting one event.
 *
 * @param event              the stored event, or {@code null} if it was a duplicate
 * @param duplicate          true if the source had already delivered this event
 * @param matchedAlertRuleIds ids of the alert rules the event matched (empty for duplicates)
 */
public record IngestionResult(EventResponse event, boolean duplicate, List<Long> matchedAlertRuleIds) {

    static IngestionResult duplicateEvent() {
        return new IngestionResult(null, true, List.of());
    }
}
