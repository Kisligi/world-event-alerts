package io.github.kisligi.alerts.event;

import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for publishing a test event by hand (admin). */
public record PublishEventRequest(
        @NotNull @Schema(example = "DISASTER") EventCategory category,
        @NotNull @Schema(example = "CRITICAL") Severity severity,
        @NotBlank @Size(max = 300) @Schema(example = "M 7.2 earthquake strikes near Tokyo") String title,
        @Size(max = 2000) @Schema(example = "Test event published from the admin view.") String description) {

    /** Source name for events published by hand. */
    public static final String SOURCE = "ADMIN";

    IncomingEvent toIncomingEvent() {
        return new IncomingEvent(SOURCE, null, category, severity, title.trim(), description, null, null);
    }
}
