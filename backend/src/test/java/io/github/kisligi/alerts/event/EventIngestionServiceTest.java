package io.github.kisligi.alerts.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import io.github.kisligi.alerts.alert.AlertRule;
import io.github.kisligi.alerts.alert.AlertRuleRepository;
import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.matching.AlertMatcher;
import io.github.kisligi.alerts.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventIngestionServiceTest {

    @Mock
    private WorldEventRepository events;

    @Mock
    private AlertRuleRepository rules;

    @Mock
    private ApplicationEventPublisher publisher;

    private EventIngestionService service;

    @BeforeEach
    void setUp() {
        // The real matcher: these tests check that ingestion applies it, not a stub.
        service = new EventIngestionService(events, rules, new AlertMatcher(), publisher);
    }

    @Test
    void duplicateFromSameSourceIsNeitherStoredNorMatched() {
        when(events.existsBySourceAndExternalId("USGS", "us1")).thenReturn(true);

        IngestionResult result = service.ingest(incoming("USGS", "us1", Severity.HIGH, "Quake"));

        assertThat(result.duplicate()).isTrue();
        assertThat(result.matchedAlertRuleIds()).isEmpty();
        verify(events, never()).save(any());
        verifyNoInteractions(rules, publisher);
    }

    @Test
    void eventWithoutExternalIdIsNeverTreatedAsDuplicate() {
        when(events.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rules.findEnabledByCategory(EventCategory.DISASTER)).thenReturn(List.of());

        IngestionResult result = service.ingest(incoming("ADMIN", null, Severity.HIGH, "Quake"));

        assertThat(result.duplicate()).isFalse();
        verify(events, never()).existsBySourceAndExternalId(any(), any());
    }

    @Test
    void storesEventAndAnnouncesOnlyTheRulesThatMatch() {
        AlertRule severeQuakes = rule(11L, Severity.HIGH, Set.of("earthquake"));
        AlertRule anyDisaster = rule(12L, Severity.LOW, Set.of());
        AlertRule floodsOnly = rule(13L, Severity.LOW, Set.of("flood"));
        AlertRule criticalOnly = rule(14L, Severity.CRITICAL, Set.of());
        when(events.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rules.findEnabledByCategory(EventCategory.DISASTER))
                .thenReturn(List.of(severeQuakes, anyDisaster, floodsOnly, criticalOnly));

        IngestionResult result = service.ingest(incoming("USGS", "us2", Severity.HIGH, "M 6.5 earthquake"));

        assertThat(result.duplicate()).isFalse();
        assertThat(result.event().title()).isEqualTo("M 6.5 earthquake");
        assertThat(result.matchedAlertRuleIds()).containsExactly(11L, 12L);
        verify(publisher).publishEvent(new WorldEventMatched(null, List.of(11L, 12L)));
    }

    @Test
    void noMatchMeansNothingIsAnnounced() {
        when(events.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rules.findEnabledByCategory(EventCategory.DISASTER)).thenReturn(List.of(rule(1L, Severity.CRITICAL, Set.of())));

        IngestionResult result = service.ingest(incoming("USGS", "us3", Severity.LOW, "Small quake"));

        assertThat(result.matchedAlertRuleIds()).isEmpty();
        verifyNoInteractions(publisher);
    }

    private static IncomingEvent incoming(String source, String externalId, Severity severity, String title) {
        return new IncomingEvent(source, externalId, EventCategory.DISASTER, severity, title, null, null, null);
    }

    /** A real rule with a fixed id; ids are normally assigned by the database. */
    private static AlertRule rule(Long id, Severity minimum, Set<String> keywords) {
        AlertRule rule = new AlertRule(new AppUser("Alice", "alice@example.com"));
        ReflectionTestUtils.setField(rule, "id", id);
        rule.replaceCategories(Set.of(EventCategory.DISASTER));
        rule.setMinSeverity(minimum);
        rule.replaceKeywords(keywords);
        return rule;
    }
}
