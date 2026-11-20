package io.github.kisligi.alerts.event.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.event.EventIngestionService;
import io.github.kisligi.alerts.event.IncomingEvent;
import io.github.kisligi.alerts.event.IngestionResult;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

class EventPollerTest {

    private final EventIngestionService ingestion = mock(EventIngestionService.class);
    private final TaskScheduler scheduler = mock(TaskScheduler.class);

    @Test
    void failingSourceIsSwallowedSoPollingContinues() {
        EventSource broken = source("BROKEN", Duration.ofMinutes(1));
        when(broken.poll()).thenThrow(new IllegalStateException("feed down"));
        EventPoller poller = new EventPoller(List.of(broken), ingestion, scheduler);

        assertThat(poller.pollOnce(broken)).isZero();
        verifyNoInteractions(ingestion);
    }

    @Test
    void oneBadEventDoesNotStopTheRestAndDuplicatesAreNotCounted() {
        IncomingEvent bad = event("bad");
        IncomingEvent fresh = event("fresh");
        IncomingEvent seen = event("seen");
        EventSource src = source("TEST", Duration.ofMinutes(1));
        when(src.poll()).thenReturn(List.of(bad, fresh, seen));
        when(ingestion.ingest(bad)).thenThrow(new IllegalArgumentException("broken event"));
        when(ingestion.ingest(fresh)).thenReturn(new IngestionResult(null, false, List.of()));
        when(ingestion.ingest(seen)).thenReturn(new IngestionResult(null, true, List.of()));
        EventPoller poller = new EventPoller(List.of(src), ingestion, scheduler);

        assertThat(poller.pollOnce(src)).isEqualTo(1);
        verify(ingestion, times(3)).ingest(any());
    }

    @Test
    void schedulesEachSourceWithItsOwnInterval() {
        EventSource fast = source("FAST", Duration.ofSeconds(30));
        EventSource slow = source("SLOW", Duration.ofMinutes(5));
        EventPoller poller = new EventPoller(List.of(fast, slow), ingestion, scheduler);

        poller.startPolling();

        verify(scheduler).scheduleWithFixedDelay(any(Runnable.class), any(Instant.class), eq(Duration.ofSeconds(30)));
        verify(scheduler).scheduleWithFixedDelay(any(Runnable.class), any(Instant.class), eq(Duration.ofMinutes(5)));
    }

    private static EventSource source(String name, Duration interval) {
        EventSource source = mock(EventSource.class);
        when(source.name()).thenReturn(name);
        when(source.pollInterval()).thenReturn(interval);
        return source;
    }

    private static IncomingEvent event(String externalId) {
        return new IncomingEvent("TEST", externalId, EventCategory.NEWS, Severity.LOW, "t", null, null, null);
    }
}
