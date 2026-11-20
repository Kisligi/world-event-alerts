package io.github.kisligi.alerts.delivery;

import java.time.Instant;

import io.github.kisligi.alerts.common.ChannelType;

/** A delivery log entry as returned by the API. */
public record DeliveryResponse(
        Long id,
        Long eventId,
        Long alertRuleId,
        String alertRuleName,
        Long userId,
        ChannelType channelType,
        String address,
        DeliveryStatus status,
        int attempts,
        String lastError,
        String subject,
        String body,
        Instant createdAt,
        Instant updatedAt) {

    static DeliveryResponse from(Delivery d) {
        return new DeliveryResponse(d.getId(), d.getEventId(), d.getAlertRuleId(), d.getAlertRuleName(),
                d.getUserId(), d.getChannelType(), d.getAddress(), d.getStatus(), d.getAttempts(),
                d.getLastError(), d.getSubject(), d.getBody(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
