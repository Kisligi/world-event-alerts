package io.github.kisligi.alerts.common;

/**
 * Notification channels a user can choose as an alert target.
 *
 * <p>Adding a channel starts here: add a constant, then implement the matching
 * channel (see {@code docs/architecture.md}, "Extension points").
 */
public enum ChannelType {
    /** {@code address} is an email address. */
    EMAIL,
    /** {@code address} is a Slack incoming-webhook URL. */
    SLACK
}
