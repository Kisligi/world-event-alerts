package io.github.kisligi.alerts.event;

import java.util.List;

/**
 * Published (as a Spring application event) when a stored world event matches one or more
 * alert rules. Notification delivery (milestone M3) listens for it.
 */
public record WorldEventMatched(Long eventId, List<Long> alertRuleIds) {
}
