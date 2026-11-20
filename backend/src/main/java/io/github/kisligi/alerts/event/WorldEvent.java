package io.github.kisligi.alerts.event;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Something that happened in the world, as received from an event source. Immutable once stored. */
@Entity
@Table(name = "world_event")
public class WorldEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 500)
    private String url;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected WorldEvent() {
        // for JPA
    }

    /** Creates an event from what a source delivered; {@code receivedAt} is now. */
    public static WorldEvent from(IncomingEvent incoming) {
        WorldEvent event = new WorldEvent();
        event.source = incoming.source();
        event.externalId = incoming.externalId();
        event.category = incoming.category();
        event.severity = incoming.severity();
        event.title = incoming.title();
        event.description = incoming.description();
        event.url = incoming.url();
        // PostgreSQL keeps microseconds; truncate so the saved and re-read values are equal.
        event.receivedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        event.occurredAt = incoming.occurredAt() == null
                ? event.receivedAt
                : incoming.occurredAt().truncatedTo(ChronoUnit.MICROS);
        return event;
    }

    /** The text that alert keywords are matched against. */
    public String searchableText() {
        return description == null ? title : title + "\n" + description;
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

    public EventCategory getCategory() {
        return category;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
