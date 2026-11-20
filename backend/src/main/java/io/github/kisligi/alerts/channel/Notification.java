package io.github.kisligi.alerts.channel;

/**
 * A channel-neutral message. Each channel decides how to render it (email subject and body,
 * Slack text, ...).
 *
 * @param subject one-line summary
 * @param body    plain-text details, several lines
 */
public record Notification(String subject, String body) {
}
