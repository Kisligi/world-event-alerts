package io.github.kisligi.alerts.event;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Events", description = "World events received from event sources")
public class EventController {

    private final WorldEventRepository events;
    private final EventIngestionService ingestion;

    public EventController(WorldEventRepository events, EventIngestionService ingestion) {
        this.events = events;
        this.ingestion = ingestion;
    }

    @GetMapping("/api/events")
    @Operation(summary = "List the most recent events, newest first")
    public List<EventResponse> recent(@RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return events.findAllByOrderByReceivedAtDescIdDesc(Limit.of(limit)).stream()
                .map(EventResponse::from)
                .toList();
    }

    @PostMapping("/api/admin/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publish a test event by hand",
            description = "Goes through the same ingestion and matching as events from real sources. "
                    + "The response lists the alert rules it matched.")
    public IngestionResult publish(@Valid @RequestBody PublishEventRequest request) {
        return ingestion.ingest(request.toIncomingEvent());
    }
}
