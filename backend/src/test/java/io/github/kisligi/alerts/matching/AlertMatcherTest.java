package io.github.kisligi.alerts.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import io.github.kisligi.alerts.alert.AlertRule;
import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.event.IncomingEvent;
import io.github.kisligi.alerts.event.WorldEvent;
import io.github.kisligi.alerts.user.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AlertMatcherTest {

    private final AlertMatcher matcher = new AlertMatcher();

    @Test
    void matchesWhenCategorySeverityAndKeywordFit() {
        AlertRule rule = rule(Set.of(EventCategory.DISASTER), Severity.HIGH, Set.of("earthquake"));

        assertThat(matcher.matches(rule, event(EventCategory.DISASTER, Severity.CRITICAL,
                "M 7.4 Earthquake near Tokyo", null))).isTrue();
    }

    @Test
    void ruleWithoutKeywordsMatchesAnyEventInItsCategories() {
        AlertRule rule = rule(Set.of(EventCategory.NEWS, EventCategory.MARKET), Severity.LOW, Set.of());

        assertThat(matcher.matches(rule, event(EventCategory.MARKET, Severity.LOW, "Anything", null))).isTrue();
        assertThat(matcher.matches(rule, event(EventCategory.NEWS, Severity.LOW, "Anything", null))).isTrue();
    }

    @Test
    void doesNotMatchOtherCategory() {
        AlertRule rule = rule(Set.of(EventCategory.MARKET), Severity.LOW, Set.of());

        assertThat(matcher.matches(rule, event(EventCategory.DISASTER, Severity.CRITICAL, "Quake", null))).isFalse();
    }

    @ParameterizedTest(name = "event {0}, rule minimum {1} -> {2}")
    @CsvSource({
            "LOW,      MEDIUM,   false",
            "MEDIUM,   MEDIUM,   true",
            "HIGH,     MEDIUM,   true",
            "HIGH,     CRITICAL, false",
            "CRITICAL, CRITICAL, true"})
    void severityMustReachTheRuleMinimum(Severity eventSeverity, Severity minimum, boolean expected) {
        AlertRule rule = rule(Set.of(EventCategory.NEWS), minimum, Set.of());

        assertThat(matcher.matches(rule, event(EventCategory.NEWS, eventSeverity, "Headline", null)))
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" / \"{1}\" -> {2}")
    @CsvSource(delimiter = '|', value = {
            "Stock MARKET CRASH                 |                              | true",
            "Index falls 12%                    | Analysts call it a crash      | true",
            "Crashes continue across Asia       |                              | true",
            "Index falls 12%                    | Trading halted                | false"})
    void keywordsAreCaseInsensitiveSubstringsOfTitleOrDescription(String title, String description,
                                                                 boolean expected) {
        AlertRule rule = rule(Set.of(EventCategory.MARKET), Severity.LOW, Set.of("crash", "sell-off"));

        assertThat(matcher.matches(rule, event(EventCategory.MARKET, Severity.HIGH, title, description)))
                .isEqualTo(expected);
    }

    @Test
    void disabledRuleNeverMatches() {
        AlertRule rule = rule(Set.of(EventCategory.NEWS), Severity.LOW, Set.of());
        rule.setEnabled(false);

        assertThat(matcher.matches(rule, event(EventCategory.NEWS, Severity.CRITICAL, "Breaking", null))).isFalse();
    }

    private static AlertRule rule(Set<EventCategory> categories, Severity minimum, Set<String> keywords) {
        AlertRule rule = new AlertRule(new AppUser("Alice", "alice@example.com"));
        rule.setName("rule");
        rule.replaceCategories(categories);
        rule.setMinSeverity(minimum);
        rule.replaceKeywords(keywords);
        return rule;
    }

    private static WorldEvent event(EventCategory category, Severity severity, String title, String description) {
        return WorldEvent.from(new IncomingEvent("TEST", null, category, severity, title, description, null, null));
    }
}
