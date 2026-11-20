package io.github.kisligi.alerts.event;

import java.util.List;

import io.github.kisligi.alerts.alert.AlertRule;
import io.github.kisligi.alerts.alert.AlertRuleRepository;
import io.github.kisligi.alerts.matching.AlertMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single entry point for new events, whatever their source: stores the event once,
 * finds the alert rules it matches, and announces the match with {@link WorldEventMatched}.
 */
@Service
public class EventIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionService.class);

    private final WorldEventRepository events;
    private final AlertRuleRepository rules;
    private final AlertMatcher matcher;
    private final ApplicationEventPublisher publisher;

    public EventIngestionService(WorldEventRepository events, AlertRuleRepository rules,
                                 AlertMatcher matcher, ApplicationEventPublisher publisher) {
        this.events = events;
        this.rules = rules;
        this.matcher = matcher;
        this.publisher = publisher;
    }

    @Transactional
    public IngestionResult ingest(IncomingEvent incoming) {
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
        if (!matched.isEmpty()) {
            publisher.publishEvent(new WorldEventMatched(event.getId(), matched));
        }
        return new IngestionResult(EventResponse.from(event), false, matched);
    }
}
