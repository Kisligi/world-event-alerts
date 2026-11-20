package io.github.kisligi.alerts.delivery;

import java.util.List;

import io.github.kisligi.alerts.common.ChannelType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    boolean existsByEventIdAndAlertRuleIdAndChannelTypeAndAddress(
            Long eventId, Long alertRuleId, ChannelType channelType, String address);

    List<Delivery> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Limit limit);

    List<Delivery> findAllByOrderByCreatedAtDescIdDesc(Limit limit);

    List<Delivery> findByStatusOrderByCreatedAtDescIdDesc(DeliveryStatus status, Limit limit);
}
