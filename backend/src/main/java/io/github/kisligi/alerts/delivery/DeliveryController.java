package io.github.kisligi.alerts.delivery;

import java.util.List;

import io.github.kisligi.alerts.common.NotFoundException;
import io.github.kisligi.alerts.user.AppUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Limit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Deliveries", description = "Notification history (user) and delivery log (admin)")
public class DeliveryController {

    private final DeliveryRepository deliveries;
    private final AppUserRepository users;

    public DeliveryController(DeliveryRepository deliveries, AppUserRepository users) {
        this.deliveries = deliveries;
        this.users = users;
    }

    @GetMapping("/api/users/{userId}/notifications")
    @Operation(summary = "A user's notification history, newest first")
    public List<DeliveryResponse> forUser(@PathVariable Long userId,
                                          @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        if (!users.existsById(userId)) {
            throw new NotFoundException("User", userId);
        }
        return deliveries.findByUserIdOrderByCreatedAtDescIdDesc(userId, Limit.of(limit)).stream()
                .map(DeliveryResponse::from)
                .toList();
    }

    @GetMapping("/api/admin/deliveries")
    @Operation(summary = "The delivery log for all users, newest first, optionally filtered by status")
    public List<DeliveryResponse> all(@RequestParam(required = false) DeliveryStatus status,
                                      @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        List<Delivery> result = status == null
                ? deliveries.findAllByOrderByCreatedAtDescIdDesc(Limit.of(limit))
                : deliveries.findByStatusOrderByCreatedAtDescIdDesc(status, Limit.of(limit));
        return result.stream().map(DeliveryResponse::from).toList();
    }
}
