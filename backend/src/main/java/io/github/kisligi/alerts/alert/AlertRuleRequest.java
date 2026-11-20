package io.github.kisligi.alerts.alert;

import java.util.List;
import java.util.Set;

import io.github.kisligi.alerts.common.ChannelType;
import io.github.kisligi.alerts.common.EventCategory;
import io.github.kisligi.alerts.common.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for creating or replacing an alert rule. */
public record AlertRuleRequest(
        @NotBlank @Size(max = 100)
        @Schema(example = "Major earthquakes")
        String name,

        @NotEmpty
        @Schema(example = "[\"DISASTER\"]")
        Set<EventCategory> categories,

        @Size(max = 20)
        @Schema(description = "Optional. Case-insensitive; the event matches if its title or "
                + "description contains any keyword. Empty means any event.",
                example = "[\"earthquake\", \"Japan\"]")
        Set<@NotBlank @Size(max = 100) String> keywords,

        @NotNull
        @Schema(example = "HIGH")
        Severity minSeverity,

        @NotEmpty @Size(max = 10)
        List<@Valid @NotNull Target> targets,

        @Schema(description = "Defaults to true.")
        Boolean enabled) {

    /** A channel target in a request. */
    public record Target(
            @NotNull @Schema(example = "EMAIL") ChannelType type,
            @NotBlank @Size(max = 500) @Schema(example = "alice@example.com") String address) {
    }
}
