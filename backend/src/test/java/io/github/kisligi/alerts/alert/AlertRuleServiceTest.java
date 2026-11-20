package io.github.kisligi.alerts.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.github.kisligi.alerts.common.ChannelType;
import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.InvalidRequestException;
import io.github.kisligi.alerts.common.NotFoundException;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.user.AppUser;
import io.github.kisligi.alerts.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertRuleServiceTest {

    @Mock
    private AlertRuleRepository rules;

    @Mock
    private AppUserRepository users;

    private AlertRuleService service;

    @BeforeEach
    void setUp() {
        service = new AlertRuleService(rules, users, new ChannelTargetValidator());
    }

    @Test
    void createsRuleWithNormalizedValuesAndEnabledByDefault() {
        when(users.findById(1L)).thenReturn(Optional.of(new AppUser("Alice", "alice@example.com")));
        when(rules.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertRuleResponse created = service.create(1L, request(
                " Quakes ", new LinkedHashSet<>(List.of(" Japan ", "japan", "EARTHQUAKE", "  ")),
                new AlertRuleRequest.Target(ChannelType.EMAIL, " alice@example.com "), null));

        assertThat(created.name()).isEqualTo("Quakes");
        assertThat(created.keywords()).containsExactly("earthquake", "japan");
        assertThat(created.targets()).containsExactly(new ChannelTarget(ChannelType.EMAIL, "alice@example.com"));
        assertThat(created.enabled()).isTrue();
    }

    @Test
    void rejectsUnknownUser() {
        when(users.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, validRequest()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User 99 not found");
        verify(rules, never()).save(any());
    }

    @Test
    void rejectsTargetAddressThatDoesNotFitChannel() {
        when(users.findById(1L)).thenReturn(Optional.of(new AppUser("Alice", "alice@example.com")));

        AlertRuleRequest bad = request("Quakes", Set.of(),
                new AlertRuleRequest.Target(ChannelType.SLACK, "alice@example.com"), true);

        assertThatThrownBy(() -> service.create(1L, bad)).isInstanceOf(InvalidRequestException.class);
        verify(rules, never()).save(any());
    }

    @Test
    void updateCanDisableRule() {
        AlertRule existing = new AlertRule(new AppUser("Alice", "alice@example.com"));
        when(rules.findById(5L)).thenReturn(Optional.of(existing));
        when(rules.saveAndFlush(existing)).thenReturn(existing);

        AlertRuleResponse updated = service.update(5L, request("Quakes", Set.of(),
                new AlertRuleRequest.Target(ChannelType.EMAIL, "alice@example.com"), false));

        assertThat(updated.enabled()).isFalse();
    }

    @Test
    void deleteOfMissingRuleIsNotFound() {
        when(rules.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(7L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Alert rule 7 not found");
    }

    private static AlertRuleRequest validRequest() {
        return request("Quakes", Set.of(),
                new AlertRuleRequest.Target(ChannelType.EMAIL, "alice@example.com"), true);
    }

    private static AlertRuleRequest request(String name, Set<String> keywords,
                                            AlertRuleRequest.Target target, Boolean enabled) {
        return new AlertRuleRequest(name, Set.of(EventCategory.DISASTER), keywords,
                Severity.HIGH, List.of(target), enabled);
    }
}
