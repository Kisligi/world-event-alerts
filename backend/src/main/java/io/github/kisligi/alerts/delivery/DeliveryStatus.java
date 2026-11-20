package io.github.kisligi.alerts.delivery;

/** Where a delivery stands. */
public enum DeliveryStatus {
    /** Created, not yet attempted. */
    PENDING,
    /** The channel accepted the message. */
    SENT,
    /** All attempts failed; {@code lastError} says why. */
    FAILED,
    /** Not attempted because the admin switched the channel off. */
    SKIPPED
}
