package io.github.kisligi.alerts.alert;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Alert rules", description = "A user's alert rules")
public class AlertRuleController {

    private final AlertRuleService service;

    public AlertRuleController(AlertRuleService service) {
        this.service = service;
    }

    @GetMapping("/api/users/{userId}/alerts")
    @Operation(summary = "List a user's alert rules")
    public List<AlertRuleResponse> list(@PathVariable Long userId) {
        return service.listForUser(userId);
    }

    @PostMapping("/api/users/{userId}/alerts")
    @Operation(summary = "Create an alert rule for a user")
    public ResponseEntity<AlertRuleResponse> create(@PathVariable Long userId,
                                                    @Valid @RequestBody AlertRuleRequest request) {
        AlertRuleResponse created = service.create(userId, request);
        return ResponseEntity.created(URI.create("/api/alerts/" + created.id())).body(created);
    }

    @GetMapping("/api/alerts/{id}")
    @Operation(summary = "Get one alert rule")
    public AlertRuleResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/api/alerts/{id}")
    @Operation(summary = "Replace an alert rule", description = "Also used to enable or disable it.")
    public AlertRuleResponse update(@PathVariable Long id, @Valid @RequestBody AlertRuleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/api/alerts/{id}")
    @Operation(summary = "Delete an alert rule")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
