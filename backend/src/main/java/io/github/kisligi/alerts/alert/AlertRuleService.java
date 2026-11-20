package io.github.kisligi.alerts.alert;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.github.kisligi.alerts.common.InvalidRequestException;
import io.github.kisligi.alerts.common.NotFoundException;
import io.github.kisligi.alerts.user.AppUser;
import io.github.kisligi.alerts.user.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates, reads, updates and deletes alert rules. */
@Service
@Transactional
public class AlertRuleService {

    private final AlertRuleRepository rules;
    private final AppUserRepository users;
    private final ChannelTargetValidator targetValidator;
    private final int maxRulesPerUser;

    public AlertRuleService(AlertRuleRepository rules, AppUserRepository users,
                            ChannelTargetValidator targetValidator,
                            @Value("${alerts.rules.max-per-user:50}") int maxRulesPerUser) {
        this.rules = rules;
        this.users = users;
        this.targetValidator = targetValidator;
        this.maxRulesPerUser = maxRulesPerUser;
    }

    @Transactional(readOnly = true)
    public List<AlertRuleResponse> listForUser(Long userId) {
        requireUser(userId);
        return rules.findAllByUserIdOrderByIdAsc(userId).stream()
                .map(AlertRuleResponse::from)
                .toList();
    }

    /** All rules of all users, for the admin view. At most users × the per-user limit. */
    @Transactional(readOnly = true)
    public List<AlertRuleResponse> listAll() {
        return rules.findAllByOrderByUserIdAscIdAsc().stream()
                .map(AlertRuleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlertRuleResponse get(Long id) {
        return AlertRuleResponse.from(requireRule(id));
    }

    public AlertRuleResponse create(Long userId, AlertRuleRequest request) {
        AppUser user = requireUser(userId);
        // Every event loads all enabled rules of its category, so the number of rules must stay
        // bounded; without login anyone could otherwise create them in a loop (review-log R7).
        // Two parallel requests can still end one above the cap, which is harmless.
        if (rules.countByUserId(userId) >= maxRulesPerUser) {
            throw new InvalidRequestException(
                    "User " + userId + " already has the maximum of " + maxRulesPerUser + " alert rules");
        }
        AlertRule rule = new AlertRule(user);
        apply(rule, request);
        return AlertRuleResponse.from(rules.save(rule));
    }

    public AlertRuleResponse update(Long id, AlertRuleRequest request) {
        AlertRule rule = requireRule(id);
        apply(rule, request);
        return AlertRuleResponse.from(rules.saveAndFlush(rule));
    }

    public void delete(Long id) {
        rules.delete(requireRule(id));
    }

    private void apply(AlertRule rule, AlertRuleRequest request) {
        Set<ChannelTarget> targets = new LinkedHashSet<>();
        for (AlertRuleRequest.Target t : request.targets()) {
            ChannelTarget target = new ChannelTarget(t.type(), t.address().trim());
            targetValidator.validate(target);
            targets.add(target);
        }
        rule.setName(request.name().trim());
        rule.replaceCategories(request.categories());
        rule.replaceKeywords(normalizeKeywords(request.keywords()));
        rule.setMinSeverity(request.minSeverity());
        rule.replaceTargets(targets);
        rule.setEnabled(request.enabled() == null || request.enabled());
    }

    /** Trims, lower-cases and de-duplicates keywords; matching is case-insensitive anyway. */
    static Set<String> normalizeKeywords(Set<String> keywords) {
        Set<String> result = new LinkedHashSet<>();
        if (keywords != null) {
            for (String keyword : keywords) {
                String normalized = keyword.trim().toLowerCase(Locale.ROOT);
                if (!normalized.isEmpty()) {
                    result.add(normalized);
                }
            }
        }
        return result;
    }

    private AppUser requireUser(Long userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
    }

    private AlertRule requireRule(Long id) {
        return rules.findById(id).orElseThrow(() -> new NotFoundException("Alert rule", id));
    }
}
