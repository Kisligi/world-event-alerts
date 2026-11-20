package io.github.kisligi.alerts.alert;

import java.util.List;

import io.github.kisligi.alerts.common.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findAllByUserIdOrderByIdAsc(Long userId);

    long countByUserId(Long userId);

    /**
     * Pre-filter for matching: enabled rules that listen to the category. Severity and keywords
     * are checked in Java by {@code AlertMatcher}, which keeps the rule logic in one place.
     */
    @Query("""
            SELECT DISTINCT r FROM AlertRule r JOIN r.categories c
            WHERE r.enabled = true AND c = :category
            ORDER BY r.id
            """)
    List<AlertRule> findEnabledByCategory(EventCategory category);
}
