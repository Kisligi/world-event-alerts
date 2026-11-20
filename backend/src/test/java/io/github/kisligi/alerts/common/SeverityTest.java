package io.github.kisligi.alerts.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SeverityTest {

    @Test
    void ranksSeveritiesInDeclarationOrder() {
        assertThat(Severity.CRITICAL.isAtLeast(Severity.HIGH)).isTrue();
        assertThat(Severity.HIGH.isAtLeast(Severity.HIGH)).isTrue();
        assertThat(Severity.MEDIUM.isAtLeast(Severity.HIGH)).isFalse();
        assertThat(Severity.LOW.isAtLeast(Severity.LOW)).isTrue();
    }
}
