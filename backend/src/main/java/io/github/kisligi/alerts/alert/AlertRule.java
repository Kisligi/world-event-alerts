package io.github.kisligi.alerts.alert;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Set;

import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.github.kisligi.alerts.user.AppUser;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * A user's alert rule: "notify me on these targets when an event of these categories,
 * at least this severe, mentioning any of these keywords, happens".
 *
 * <p>An empty keyword set means "any event in the selected categories".
 */
@Entity
@Table(name = "alert_rule")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 100)
    private String name;

    @ElementCollection
    @CollectionTable(name = "alert_rule_category", joinColumns = @JoinColumn(name = "alert_rule_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private Set<EventCategory> categories = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "alert_rule_keyword", joinColumns = @JoinColumn(name = "alert_rule_id"))
    @Column(name = "keyword", nullable = false, length = 100)
    private Set<String> keywords = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "min_severity", nullable = false, length = 20)
    private Severity minSeverity;

    @ElementCollection
    @CollectionTable(name = "alert_rule_target", joinColumns = @JoinColumn(name = "alert_rule_id"))
    private Set<ChannelTarget> targets = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AlertRule() {
        // for JPA
    }

    public AlertRule(AppUser user) {
        this.user = user;
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

    /**
     * PostgreSQL stores timestamps with microsecond precision. Truncating here keeps the value
     * returned right after a save identical to the value read back from the database later.
     */
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<EventCategory> getCategories() {
        return categories;
    }

    public void replaceCategories(Set<EventCategory> newCategories) {
        categories.clear();
        categories.addAll(newCategories);
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public void replaceKeywords(Set<String> newKeywords) {
        keywords.clear();
        keywords.addAll(newKeywords);
    }

    public Severity getMinSeverity() {
        return minSeverity;
    }

    public void setMinSeverity(Severity minSeverity) {
        this.minSeverity = minSeverity;
    }

    public Set<ChannelTarget> getTargets() {
        return targets;
    }

    public void replaceTargets(Set<ChannelTarget> newTargets) {
        targets.clear();
        targets.addAll(newTargets);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
