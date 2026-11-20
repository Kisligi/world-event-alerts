package io.github.kisligi.alerts.channel;

import io.github.kisligi.alerts.common.ChannelType;

/**
 * A way to deliver a notification. Every implementation is a Spring bean; the dispatcher
 * finds the one whose {@link #type()} matches an alert target. To add a channel (say, Teams):
 * add a {@link ChannelType} constant and its address validation, implement this interface, and
 * seed a {@code channel_setting} row. Nothing in matching or dispatch changes. See
 * docs/architecture.md, "Extension points".
 */
public interface NotificationChannel {

    ChannelType type();

    /**
     * Sends one notification to one address (an email address, a webhook URL, ...).
     *
     * @throws DeliveryException if the message could not be delivered; the dispatcher retries
     */
    void send(String address, Notification notification);
}
