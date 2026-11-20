package io.github.kisligi.alerts.alert;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;

/** An alert rule as returned by the API. Collections are sorted so responses are stable. */
public record AlertRuleResponse(
        Long id,
        Long userId,
        String name,
        List<EventCategory> categories,
        List<String> keywords,
        Severity minSeverity,
        List<ChannelTarget> targets,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    private static final Comparator<ChannelTarget> TARGET_ORDER =
            Comparator.comparing(ChannelTarget::type).thenComparing(ChannelTarget::address);

    static AlertRuleResponse from(AlertRule rule) {
        return new AlertRuleResponse(
                rule.getId(),
                rule.getUser().getId(),
                rule.getName(),
                rule.getCategories().stream().sorted().toList(),
                rule.getKeywords().stream().sorted().toList(),
                rule.getMinSeverity(),
                rule.getTargets().stream().sorted(TARGET_ORDER).toList(),
                rule.isEnabled(),
                rule.getCreatedAt(),
                rule.getUpdatedAt());
    }
}
