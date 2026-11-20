package io.github.kisligi.alerts.matching;

import java.util.Locale;

import io.github.kisligi.alerts.alert.AlertRule;
import io.github.kisligi.alerts.event.WorldEvent;
import org.springframework.stereotype.Component;

/**
 * Decides whether a world event matches an alert rule. This is the single place where the
 * matching rules live (docs/decisions.md ADR-006):
 * <ol>
 *   <li>the rule is enabled;</li>
 *   <li>the event's category is one of the rule's categories;</li>
 *   <li>the event is at least as severe as the rule's minimum severity;</li>
 *   <li>the rule has no keywords, or at least one keyword appears in the event's title or
 *       description (case-insensitive substring, so "crash" also matches "crashes").</li>
 * </ol>
 */
@Component
public class AlertMatcher {

    public boolean matches(AlertRule rule, WorldEvent event) {
        return rule.isEnabled()
                && rule.getCategories().contains(event.getCategory())
                && event.getSeverity().isAtLeast(rule.getMinSeverity())
                && keywordsMatch(rule, event);
    }

    private static boolean keywordsMatch(AlertRule rule, WorldEvent event) {
        if (rule.getKeywords().isEmpty()) {
            return true;
        }
        String text = event.searchableText().toLowerCase(Locale.ROOT);
        // Keywords are stored lower-case (ADR-017).
        return rule.getKeywords().stream().anyMatch(text::contains);
    }
}
