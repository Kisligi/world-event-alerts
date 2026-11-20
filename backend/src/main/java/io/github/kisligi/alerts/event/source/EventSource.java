package io.github.kisligi.alerts.event.source;

import java.time.Duration;
import java.util.List;

import io.github.kisligi.alerts.event.IncomingEvent;

/**
 * A place world events come from. Implementations are Spring beans; {@link EventPoller} asks
 * each enabled source for new events at its own interval and hands them to ingestion.
 *
 * <p>To add a source: implement this interface, make it a bean (usually enabled by a config
 * property), and return events with a stable {@code externalId} so repeats are ignored.
 */
public interface EventSource {

    /** Short, stable name stored with each event, e.g. {@code SIMULATOR}. */
    String name();

    /** How long to wait between two polls. */
    Duration pollInterval();

    /**
     * Returns the events currently available. May return events already delivered before;
     * ingestion drops them by {@code (source, externalId)}. May throw; the poller logs it and
     * tries again next time.
     */
    List<IncomingEvent> poll();
}
