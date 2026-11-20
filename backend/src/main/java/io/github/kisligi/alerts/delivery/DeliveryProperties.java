package io.github.kisligi.alerts.delivery;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Retry settings under {@code alerts.delivery} (docs/decisions.md ADR-007): up to
 * {@code maxAttempts} tries, waiting {@code initialBackoff}, then {@code initialBackoff × multiplier}, ...
 */
@ConfigurationProperties("alerts.delivery")
public record DeliveryProperties(
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("1s") Duration initialBackoff,
        @DefaultValue("2") double multiplier) {

    /** Wait before attempt {@code nextAttempt} (2, 3, ...). */
    Duration backoffBefore(int nextAttempt) {
        double factor = Math.pow(multiplier, nextAttempt - 2);
        return Duration.ofMillis(Math.round(initialBackoff.toMillis() * factor));
    }
}
