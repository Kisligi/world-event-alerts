package io.github.kisligi.alerts.common;

/**
 * How important an event is. Declaration order defines the ranking:
 * {@code LOW < MEDIUM < HIGH < CRITICAL}.
 */
public enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /** Returns true if this severity is the same as or more severe than {@code other}. */
    public boolean isAtLeast(Severity other) {
        return compareTo(other) >= 0;
    }
}
