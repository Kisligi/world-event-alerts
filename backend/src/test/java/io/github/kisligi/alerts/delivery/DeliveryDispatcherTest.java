package io.github.kisligi.alerts.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.github.kisligi.alerts.alert.AlertRule;
import io.github.kisligi.alerts.alert.AlertRuleRepository;
import io.github.kisligi.alerts.alert.ChannelTarget;
import io.github.kisligi.alerts.channel.ChannelSettingService;
import io.github.kisligi.alerts.channel.DeliveryException;
import io.github.kisligi.alerts.channel.Notification;
import io.github.kisligi.alerts.channel.NotificationChannel;
import io.github.kisligi.alerts.common.ChannelType;
import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.event.IncomingEvent;
import io.github.kisligi.alerts.event.WorldEvent;
import io.github.kisligi.alerts.event.WorldEventMatched;
import io.github.kisligi.alerts.event.WorldEventRepository;
import io.github.kisligi.alerts.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class DeliveryDispatcherTest {

    private static final ChannelTarget ALICE_MAIL = new ChannelTarget(ChannelType.EMAIL, "alice@example.com");
    private static final ChannelTarget ALICE_SLACK = new ChannelTarget(ChannelType.SLACK, "http://slack/services/a");

    private final WorldEventRepository events = mock(WorldEventRepository.class);
    private final AlertRuleRepository rules = mock(AlertRuleRepository.class);
    private final DeliveryRepository deliveries = mock(DeliveryRepository.class);
    private final ChannelSettingService settings = mock(ChannelSettingService.class);
    private final List<Duration> waits = new ArrayList<>();

    private FakeChannel email;
    private FakeChannel slack;
    private WorldEvent event;

    @BeforeEach
    void setUp() {
        email = new FakeChannel(ChannelType.EMAIL);
        slack = new FakeChannel(ChannelType.SLACK);
        event = WorldEvent.from(new IncomingEvent("ADMIN", null, EventCategory.DISASTER, Severity.HIGH,
                "Quake", null, null, null));
        ReflectionTestUtils.setField(event, "id", 100L);
        when(events.findById(100L)).thenReturn(Optional.of(event));
        when(settings.isEnabled(any())).thenReturn(true);
        when(deliveries.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private DeliveryDispatcher dispatcher(NotificationChannel... channels) {
        return new DeliveryDispatcher(events, rules, deliveries, settings, List.of(channels),
                new NotificationFormatter(), new DeliveryProperties(3, Duration.ofSeconds(1), 2), waits::add,
                new TransactionTemplate(mock(PlatformTransactionManager.class)));
    }

    @Test
    void sendsToEveryTargetOfEveryMatchedRuleAndRecordsSuccess() {
        givenRules(rule(1L, "Quakes", ALICE_MAIL, ALICE_SLACK));

        List<Delivery> result = dispatcher(email, slack).dispatch(new WorldEventMatched(100L, List.of(1L)));

        assertThat(result).extracting(Delivery::getStatus).containsOnly(DeliveryStatus.SENT);
        assertThat(result).extracting(Delivery::getAttempts).containsOnly(1);
        assertThat(email.sent).containsExactly("alice@example.com");
        assertThat(slack.sent).containsExactly("http://slack/services/a");
        assertThat(email.lastNotification.subject()).isEqualTo("[HIGH] Disaster: Quake");
        assertThat(result.getFirst().getAlertRuleName()).isEqualTo("Quakes");
        assertThat(result.getFirst().getUserId()).isEqualTo(7L);
        assertThat(waits).isEmpty();
    }

    @Test
    void existingDeliveryIsNotSentAgain() {
        givenRules(rule(1L, "Quakes", ALICE_MAIL));
        when(deliveries.existsByEventIdAndAlertRuleIdAndChannelTypeAndAddress(100L, 1L, ChannelType.EMAIL,
                "alice@example.com")).thenReturn(true);

        assertThat(dispatcher(email).dispatch(new WorldEventMatched(100L, List.of(1L)))).isEmpty();
        assertThat(email.sent).isEmpty();
        verify(deliveries, never()).save(any());
    }

    @Test
    void concurrentInsertOfTheSameDeliveryIsTreatedAsDuplicate() {
        givenRules(rule(1L, "Quakes", ALICE_MAIL));
        when(deliveries.save(any())).thenThrow(new DataIntegrityViolationException("uq_delivery_event_rule_target"));

        assertThat(dispatcher(email).dispatch(new WorldEventMatched(100L, List.of(1L)))).isEmpty();
        assertThat(email.sent).isEmpty();
    }

    @Test
    void transientFailureIsRetriedWithBackoffUntilItSucceeds() {
        givenRules(rule(1L, "Quakes", ALICE_MAIL));
        email.failuresBeforeSuccess = 2;

        Delivery delivery = dispatcher(email).dispatch(new WorldEventMatched(100L, List.of(1L))).getFirst();

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(delivery.getAttempts()).isEqualTo(3);
        assertThat(delivery.getLastError()).isNull();
        assertThat(waits).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(2));
    }

    @Test
    void permanentFailureStopsAfterMaxAttemptsWithLastError() {
        givenRules(rule(1L, "Quakes", ALICE_MAIL, ALICE_SLACK));
        email.failuresBeforeSuccess = Integer.MAX_VALUE;

        List<Delivery> result = dispatcher(email, slack).dispatch(new WorldEventMatched(100L, List.of(1L)));

        Delivery failed = result.getFirst();
        assertThat(failed.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(failed.getAttempts()).isEqualTo(3);
        assertThat(failed.getLastError()).isEqualTo("EMAIL failure 3");
        // One broken channel does not stop the others.
        assertThat(result.get(1).getStatus()).isEqualTo(DeliveryStatus.SENT);
    }

    @Test
    void unexpectedExceptionWithoutMessageIsRecordedByItsType() {
        givenRules(rule(1L, "Quakes", ALICE_MAIL));
        NotificationChannel buggy = new NotificationChannel() {
            @Override
            public ChannelType type() {
                return ChannelType.EMAIL;
            }

            @Override
            public void send(String address, Notification notification) {
                throw new NullPointerException();
            }
        };

        Delivery delivery = dispatcher(buggy).dispatch(new WorldEventMatched(100L, List.of(1L))).getFirst();

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getLastError()).isEqualTo("NullPointerException");
    }

    @Test
    void switchedOffChannelIsRecordedAsSkippedAndNotCalled() {
        givenRules(rule(1L, "Quakes", ALICE_MAIL, ALICE_SLACK));
        when(settings.isEnabled(ChannelType.SLACK)).thenReturn(false);

        List<Delivery> result = dispatcher(email, slack).dispatch(new WorldEventMatched(100L, List.of(1L)));

        assertThat(result).extracting(Delivery::getStatus).containsExactly(DeliveryStatus.SENT, DeliveryStatus.SKIPPED);
        assertThat(result.get(1).getLastError()).isEqualTo("Channel SLACK is switched off by the admin");
        assertThat(result.get(1).getAttempts()).isZero();
        assertThat(slack.sent).isEmpty();
    }

    @Test
    void targetWithoutChannelImplementationFailsWithoutAttempts() {
        givenRules(rule(1L, "Quakes", ALICE_SLACK));

        Delivery delivery = dispatcher(email).dispatch(new WorldEventMatched(100L, List.of(1L))).getFirst();

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getAttempts()).isZero();
        assertThat(delivery.getLastError()).isEqualTo("No channel implementation for SLACK");
    }

    @Test
    void ruleDeletedSinceMatchingIsIgnored() {
        when(rules.findAllById(List.of(1L))).thenReturn(List.of());

        assertThat(dispatcher(email).dispatch(new WorldEventMatched(100L, List.of(1L)))).isEmpty();
        assertThat(email.sent).isEmpty();
    }

    @Test
    void eventDeletedSinceMatchingSendsNothing() {
        when(events.findById(100L)).thenReturn(Optional.empty());

        assertThat(dispatcher(email).dispatch(new WorldEventMatched(100L, List.of(1L)))).isEmpty();
    }

    @Test
    void interruptedWaitStopsRetryingAndKeepsTheInterruptFlag() {
        givenRules(rule(1L, "Quakes", ALICE_MAIL));
        email.failuresBeforeSuccess = Integer.MAX_VALUE;
        DeliveryDispatcher interrupting = new DeliveryDispatcher(events, rules, deliveries, settings, List.of(email),
                new NotificationFormatter(), new DeliveryProperties(3, Duration.ofSeconds(1), 2),
                duration -> {
                    throw new InterruptedException();
                },
                new TransactionTemplate(mock(PlatformTransactionManager.class)));

        try {
            Delivery delivery = interrupting.dispatch(new WorldEventMatched(100L, List.of(1L))).getFirst();

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(delivery.getAttempts()).isEqualTo(1);
            assertThat(delivery.getLastError()).endsWith("(retries interrupted)");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted(); // clear the flag for other tests
        }
    }

    @Test
    void veryLongErrorIsCutToFitTheColumn() {
        Delivery delivery = new Delivery(1L, 1L, "r", 1L, ChannelType.EMAIL, "a@example.com", "s", "b");

        delivery.markFailed(3, "x".repeat(5000));

        assertThat(delivery.getLastError()).hasSize(1000).endsWith("…");
    }

    private void givenRules(AlertRule... matchedRules) {
        List<Long> ids = java.util.Arrays.stream(matchedRules).map(AlertRule::getId).toList();
        when(rules.findAllById(ids)).thenReturn(List.of(matchedRules));
    }

    private static AlertRule rule(Long id, String name, ChannelTarget... targets) {
        AppUser user = new AppUser("Alice", "alice@example.com");
        ReflectionTestUtils.setField(user, "id", 7L);
        AlertRule rule = new AlertRule(user);
        ReflectionTestUtils.setField(rule, "id", id);
        rule.setName(name);
        rule.replaceCategories(Set.of(EventCategory.DISASTER));
        rule.setMinSeverity(Severity.LOW);
        // Keep the given order so assertions can rely on it.
        rule.getTargets().clear();
        for (ChannelTarget target : targets) {
            rule.getTargets().add(target);
        }
        return rule;
    }

    /** A channel that records what it sent and can fail a given number of times first. */
    private static final class FakeChannel implements NotificationChannel {
        private final ChannelType type;
        private final List<String> sent = new ArrayList<>();
        private int failuresBeforeSuccess;
        private int calls;
        private Notification lastNotification;

        FakeChannel(ChannelType type) {
            this.type = type;
        }

        @Override
        public ChannelType type() {
            return type;
        }

        @Override
        public void send(String address, Notification notification) {
            calls++;
            if (calls <= failuresBeforeSuccess) {
                throw new DeliveryException(type + " failure " + calls, null);
            }
            sent.add(address);
            lastNotification = notification;
        }
    }
}
