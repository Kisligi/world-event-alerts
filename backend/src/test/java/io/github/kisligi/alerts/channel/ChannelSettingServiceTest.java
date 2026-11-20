package io.github.kisligi.alerts.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.github.kisligi.alerts.channel.ChannelSettingService.ChannelStatus;
import io.github.kisligi.alerts.common.ChannelType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChannelSettingServiceTest {

    private final ChannelSettingRepository repository = mock(ChannelSettingRepository.class);

    private ChannelSettingService serviceWith(NotificationChannel... channels) {
        return new ChannelSettingService(repository, List.of(channels));
    }

    @Test
    void channelWithoutStoredSettingCountsAsEnabled() {
        when(repository.findById(ChannelType.SLACK)).thenReturn(Optional.empty());

        assertThat(serviceWith().isEnabled(ChannelType.SLACK)).isTrue();
    }

    @Test
    void listShowsSwitchAndWhetherAnImplementationExists() {
        when(repository.findById(ChannelType.EMAIL)).thenReturn(Optional.of(new ChannelSetting(ChannelType.EMAIL, false)));
        when(repository.findById(ChannelType.SLACK)).thenReturn(Optional.of(new ChannelSetting(ChannelType.SLACK, true)));
        NotificationChannel email = mock(NotificationChannel.class);
        when(email.type()).thenReturn(ChannelType.EMAIL);

        assertThat(serviceWith(email).list()).containsExactly(
                new ChannelStatus(ChannelType.EMAIL, false, true),
                new ChannelStatus(ChannelType.SLACK, true, false));
    }

    @Test
    void switchingOffCreatesSettingIfMissing() {
        when(repository.findById(ChannelType.SLACK)).thenReturn(Optional.empty());

        ChannelStatus status = serviceWith().setEnabled(ChannelType.SLACK, false);

        ArgumentCaptor<ChannelSetting> saved = ArgumentCaptor.forClass(ChannelSetting.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getChannelType()).isEqualTo(ChannelType.SLACK);
        assertThat(saved.getValue().isEnabled()).isFalse();
        assertThat(status.enabled()).isFalse();
    }

    @Test
    void switchingUpdatesExistingSetting() {
        ChannelSetting existing = new ChannelSetting(ChannelType.EMAIL, false);
        when(repository.findById(ChannelType.EMAIL)).thenReturn(Optional.of(existing));

        serviceWith().setEnabled(ChannelType.EMAIL, true);

        assertThat(existing.isEnabled()).isTrue();
        verify(repository).save(any());
    }
}
