package io.github.kisligi.alerts.alert;

import io.github.kisligi.alerts.common.ChannelType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Where to deliver a notification: a channel type plus a channel-specific address
 * (an email address for EMAIL, a webhook URL for SLACK).
 */
@Embeddable
public record ChannelTarget(
        @Enumerated(EnumType.STRING) @Column(name = "channel_type", nullable = false, length = 20)
        ChannelType type,
        @Column(name = "address", nullable = false, length = 500)
        String address) {
}
