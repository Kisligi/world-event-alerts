package io.github.kisligi.alerts.event;

import java.util.List;

import io.github.kisligi.alerts.alert.AlertRule;
import io.github.kisligi.alerts.alert.AlertRuleRepository;
import io.github.kisligi.alerts.matching.AlertMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The single entry point for new events, whatever their source: stores the event once,
 * finds the alert rules it matches, and announces the match with {@link WorldEventMatched}.
 *
 * <p>The match is announced only after the transaction has finished and its database
 * connection is back in the pool. Delivery listens for the announcement and can take seconds
 * (network calls, retries); if it ran while this transaction's connection was still held, and
 * each delivery step borrowed a second one, a few concurrent events could exhaust the pool
 * (docs/decisions.md ADR-023, found in the M3 review).
 */
@Service
public class EventIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionService.class);

    private final WorldEventRepository events;
    private final AlertRuleRepository rules;
    private final AlertMatcher matcher;
    private final ApplicationEventPublisher publisher;
    private final TransactionTemplate transaction;

    public EventIngestionService(WorldEventRepository events, AlertRuleRepository rules, AlertMatcher matcher,
                                 ApplicationEventPublisher publisher, TransactionTemplate transaction) {
        this.events = events;
        this.rules = rules;
        this.matcher = matcher;
        this.publisher = publisher;
        this.transaction = transaction;
    }

    public IngestionResult ingest(IncomingEvent incoming) {
        IngestionResult result = transaction.execute(status -> storeAndMatch(incoming));
        // Outside the transaction on purpose: see the class comment.
        if (!result.matchedAlertRuleIds().isEmpty()) {
            publisher.publishEvent(new WorldEventMatched(result.event().id(), result.matchedAlertRuleIds()));
        }
        return result;
    }

    private IngestionResult storeAndMatch(IncomingEvent incoming) {
        if (incoming.externalId() != null
                && events.existsBySourceAndExternalId(incoming.source(), incoming.externalId())) {
            log.debug("Skipping duplicate event {}/{}", incoming.source(), incoming.externalId());
            return IngestionResult.duplicateEvent();
        }

        WorldEvent event = events.save(WorldEvent.from(incoming));
        List<Long> matched = rules.findEnabledByCategory(event.getCategory()).stream()
                .filter(rule -> matcher.matches(rule, event))
                .map(AlertRule::getId)
                .toList();

        log.info("Event {} [{} {} from {}] \"{}\" matched {} alert rule(s)", event.getId(),
                event.getCategory(), event.getSeverity(), event.getSource(), event.getTitle(), matched.size());
        return new IngestionResult(EventResponse.from(event), false, matched);
    }
}
