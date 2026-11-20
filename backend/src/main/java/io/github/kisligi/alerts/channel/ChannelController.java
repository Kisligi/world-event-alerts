package io.github.kisligi.alerts.channel;

import java.util.List;

import io.github.kisligi.alerts.channel.ChannelSettingService.ChannelStatus;
import io.github.kisligi.alerts.common.ChannelType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin: channels", description = "Global on/off switches for notification channels")
public class ChannelController {

    private final ChannelSettingService service;

    public ChannelController(ChannelSettingService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/channels")
    @Operation(summary = "List channels with their global switch")
    public List<ChannelStatus> list() {
        return service.list();
    }

    @PutMapping("/api/admin/channels/{type}")
    @Operation(summary = "Switch a channel on or off for everyone",
            description = "While a channel is off, its deliveries are recorded as SKIPPED and nothing is sent.")
    public ChannelStatus update(@PathVariable ChannelType type, @Valid @RequestBody Toggle toggle) {
        return service.setEnabled(type, toggle.enabled());
    }

    /** Request body for {@link #update}. */
    public record Toggle(@NotNull Boolean enabled) {
    }
}
