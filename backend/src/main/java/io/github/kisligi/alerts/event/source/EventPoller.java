package io.github.kisligi.alerts.event.source;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import io.github.kisligi.alerts.event.EventIngestionService;
import io.github.kisligi.alerts.event.IncomingEvent;
import io.github.kisligi.alerts.event.IngestionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

/**
 * Polls every enabled {@link EventSource} at its own interval and passes the events to
 * ingestion. A failing source is logged and retried at its next poll; it never stops the
 * other sources. Switched off in tests with {@code alerts.sources.polling-enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "alerts.sources.polling-enabled", havingValue = "true", matchIfMissing = true)
public class EventPoller {

    private static final Logger log = LoggerFactory.getLogger(EventPoller.class);

    /** Give the app a moment after startup before the first poll. */
    private static final Duration INITIAL_DELAY = Duration.ofSeconds(5);

    private final List<EventSource> sources;
    private final EventIngestionService ingestion;
    private final TaskScheduler scheduler;

    public EventPoller(List<EventSource> sources, EventIngestionService ingestion, TaskScheduler scheduler) {
        this.sources = sources;
        this.ingestion = ingestion;
        this.scheduler = scheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    void startPolling() {
        for (EventSource source : sources) {
            log.info("Polling event source {} every {}", source.name(), source.pollInterval());
            scheduler.scheduleWithFixedDelay(() -> pollOnce(source),
                    Instant.now().plus(INITIAL_DELAY), source.pollInterval());
        }
    }

    /**
     * Polls one source once and ingests what it returns.
     *
     * @return how many new (non-duplicate) events were stored
     */
    int pollOnce(EventSource source) {
        List<IncomingEvent> incoming;
        try {
            incoming = source.poll();
        } catch (RuntimeException e) {
            log.warn("Event source {} failed: {}", source.name(), e.toString());
            return 0;
        }
        int stored = 0;
        for (IncomingEvent event : incoming) {
            try {
                IngestionResult result = ingestion.ingest(event);
                if (!result.duplicate()) {
                    stored++;
                }
            } catch (RuntimeException e) {
                log.warn("Could not ingest event {} from {}: {}", event.externalId(), source.name(), e.toString());
            }
        }
        return stored;
    }
}
