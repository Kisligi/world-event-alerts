package io.github.kisligi.alerts.delivery;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.github.kisligi.alerts.common.ChannelType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * One notification for one (event, alert rule, channel target): the delivery log entry.
 * Ids are stored as plain columns so a delivery survives as history when its rule is deleted.
 */
@Entity
@Table(name = "delivery")
public class Delivery {

    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "alert_rule_id")
    private Long alertRuleId;

    @Column(name = "alert_rule_name", nullable = false, length = 100)
    private String alertRuleName;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Column(nullable = false, length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @Column(nullable = false, length = 400)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Delivery() {
        // for JPA
    }

    public Delivery(Long eventId, Long alertRuleId, String alertRuleName, Long userId,
                    ChannelType channelType, String address, String subject, String body) {
        this.eventId = eventId;
        this.alertRuleId = alertRuleId;
        this.alertRuleName = alertRuleName;
        this.userId = userId;
        this.channelType = channelType;
        this.address = address;
        this.subject = subject;
        this.body = body;
    }

    @PrePersist
    void onCreate() {
        createdAt = now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = now();
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    void markSent(int attempts) {
        this.status = DeliveryStatus.SENT;
        this.attempts = attempts;
        this.lastError = null;
    }

    /** @param error why it failed; never null (the dispatcher always has a reason) */
    void markFailed(int attempts, String error) {
        this.status = DeliveryStatus.FAILED;
        this.attempts = attempts;
        this.lastError = error.length() <= MAX_ERROR_LENGTH
                ? error
                : error.substring(0, MAX_ERROR_LENGTH - 1) + "…";
    }

    void markSkipped(String reason) {
        this.status = DeliveryStatus.SKIPPED;
        this.lastError = reason;
    }

    public Long getId() {
        return id;
    }

    public Long getEventId() {
        return eventId;
    }

    public Long getAlertRuleId() {
        return alertRuleId;
    }

    public String getAlertRuleName() {
        return alertRuleName;
    }

    public Long getUserId() {
        return userId;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public String getAddress() {
        return address;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
