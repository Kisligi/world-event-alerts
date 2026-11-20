package io.github.kisligi.alerts.channel;

import io.github.kisligi.alerts.common.ChannelType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Global on/off switch for one channel type, controlled by the admin. */
@Entity
@Table(name = "channel_setting")
public class ChannelSetting {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", length = 20)
    private ChannelType channelType;

    @Column(nullable = false)
    private boolean enabled;

    protected ChannelSetting() {
        // for JPA
    }

    public ChannelSetting(ChannelType channelType, boolean enabled) {
        this.channelType = channelType;
        this.enabled = enabled;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
