package io.github.kisligi.alerts.channel;

import io.github.kisligi.alerts.common.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelSettingRepository extends JpaRepository<ChannelSetting, ChannelType> {
}
