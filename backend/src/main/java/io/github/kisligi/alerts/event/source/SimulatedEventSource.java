package io.github.kisligi.alerts.event.source;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;

import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.event.IncomingEvent;

/**
 * Emits one sample event per poll, picked at random from a fixed catalog covering every
 * category and severity. Every event is labelled as simulated. Makes the demo work offline
 * and without API keys (docs/decisions.md ADR-003).
 */
public class SimulatedEventSource implements EventSource {

    public static final String NAME = "SIMULATOR";

    private static final String LABEL = "[SIMULATED] This event was generated for demonstration purposes.";

    /** One possible simulated event. */
    record Template(EventCategory category, Severity severity, String title) {
    }

    static final List<Template> CATALOG = List.of(
            new Template(EventCategory.NEWS, Severity.LOW, "City council approves new cycling lanes"),
            new Template(EventCategory.NEWS, Severity.MEDIUM, "Nationwide rail strike announced for next week"),
            new Template(EventCategory.NEWS, Severity.HIGH, "Major cyberattack disrupts airport check-in systems"),
            new Template(EventCategory.NEWS, Severity.CRITICAL, "Breaking: emergency summit called after border clash"),
            new Template(EventCategory.MARKET, Severity.LOW, "Tech index edges up 0.3% in quiet trading"),
            new Template(EventCategory.MARKET, Severity.MEDIUM, "Oil prices jump 4% after supply disruption"),
            new Template(EventCategory.MARKET, Severity.HIGH, "Central bank raises interest rates by 75 basis points"),
            new Template(EventCategory.MARKET, Severity.CRITICAL, "Market crash: stock index falls 12%, trading halted"),
            new Template(EventCategory.DISASTER, Severity.LOW, "Minor M 3.1 earthquake felt in coastal towns"),
            new Template(EventCategory.DISASTER, Severity.MEDIUM, "Flood warning issued as river levels rise"),
            new Template(EventCategory.DISASTER, Severity.HIGH, "Wildfire forces evacuation of 5,000 residents"),
            new Template(EventCategory.DISASTER, Severity.CRITICAL, "M 7.4 earthquake and tsunami warning in the Pacific"));

    private final RandomGenerator random;
    private final Duration interval;

    public SimulatedEventSource(Duration interval) {
        this(new Random(), interval);
    }

    /** For tests: a seeded generator makes the output repeatable. */
    SimulatedEventSource(RandomGenerator random, Duration interval) {
        this.random = random;
        this.interval = interval;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Duration pollInterval() {
        return interval;
    }

    @Override
    public List<IncomingEvent> poll() {
        Template template = CATALOG.get(random.nextInt(CATALOG.size()));
        return List.of(new IncomingEvent(NAME, UUID.randomUUID().toString(), template.category(),
                template.severity(), template.title(), LABEL, null, null));
    }
}
