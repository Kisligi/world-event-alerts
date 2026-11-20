package io.github.kisligi.alerts.event;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorldEventRepository extends JpaRepository<WorldEvent, Long> {

    boolean existsBySourceAndExternalId(String source, String externalId);

    List<WorldEvent> findAllByOrderByReceivedAtDescIdDesc(Limit limit);
}
