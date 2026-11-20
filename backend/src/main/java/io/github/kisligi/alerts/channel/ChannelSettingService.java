package io.github.kisligi.alerts.channel;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.kisligi.alerts.common.ChannelType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and changes the global channel switches. A channel without a row counts as enabled. */
@Service
public class ChannelSettingService {

    private final ChannelSettingRepository settings;
    private final Set<ChannelType> implemented;

    public ChannelSettingService(ChannelSettingRepository settings, List<NotificationChannel> channels) {
        this.settings = settings;
        this.implemented = channels.stream().map(NotificationChannel::type).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(ChannelType type) {
        return settings.findById(type).map(ChannelSetting::isEnabled).orElse(true);
    }

    @Transactional(readOnly = true)
    public List<ChannelStatus> list() {
        return Arrays.stream(ChannelType.values())
                .map(type -> new ChannelStatus(type, isEnabled(type), implemented.contains(type)))
                .toList();
    }

    @Transactional
    public ChannelStatus setEnabled(ChannelType type, boolean enabled) {
        ChannelSetting setting = settings.findById(type).orElseGet(() -> new ChannelSetting(type, enabled));
        setting.setEnabled(enabled);
        settings.save(setting);
        return new ChannelStatus(type, enabled, implemented.contains(type));
    }

    /**
     * A channel as shown to the admin.
     *
     * @param implemented whether a {@link NotificationChannel} bean exists for this type
     */
    public record ChannelStatus(ChannelType type, boolean enabled, boolean implemented) {
    }
}
