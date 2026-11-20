package io.github.kisligi.alerts.event.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.event.IncomingEvent;
import org.junit.jupiter.api.Test;

class SimulatedEventSourceTest {

    @Test
    void catalogCoversEveryCategoryAndSeverity() {
        // Otherwise some alert rules could never be triggered in the demo.
        Set<String> combinations = new HashSet<>();
        SimulatedEventSource.CATALOG.forEach(t -> combinations.add(t.category() + "/" + t.severity()));

        assertThat(combinations).hasSize(EventCategory.values().length * Severity.values().length);
    }

    @Test
    void eachPollReturnsOneLabelledEventWithAUniqueExternalId() {
        SimulatedEventSource source = new SimulatedEventSource(new Random(42), Duration.ofSeconds(30));

        List<IncomingEvent> events = IntStream.range(0, 50).mapToObj(i -> source.poll())
                .peek(batch -> assertThat(batch).hasSize(1))
                .map(batch -> batch.getFirst())
                .toList();

        assertThat(events).allSatisfy(e -> {
            assertThat(e.source()).isEqualTo("SIMULATOR");
            assertThat(e.description()).startsWith("[SIMULATED]");
            assertThat(e.title()).isNotBlank();
        });
        assertThat(events).extracting(IncomingEvent::externalId).doesNotHaveDuplicates();
    }

    @Test
    void sameSeedGivesSameSequence() {
        List<String> first = titles(new SimulatedEventSource(new Random(7), Duration.ofSeconds(1)));
        List<String> second = titles(new SimulatedEventSource(new Random(7), Duration.ofSeconds(1)));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void reportsConfiguredNameAndInterval() {
        SimulatedEventSource source = new SimulatedEventSource(Duration.ofSeconds(12));

        assertThat(source.name()).isEqualTo("SIMULATOR");
        assertThat(source.pollInterval()).isEqualTo(Duration.ofSeconds(12));
    }

    private static List<String> titles(SimulatedEventSource source) {
        return IntStream.range(0, 20).mapToObj(i -> source.poll().getFirst().title()).toList();
    }
}
