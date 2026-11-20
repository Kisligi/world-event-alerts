package io.github.kisligi.alerts.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.kisligi.alerts.common.ChannelType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailChannelTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailChannel channel = new EmailChannel(mailSender, "Alerts <alerts@example.com>");

    @Test
    void sendsPlainTextMailFromConfiguredSender() {
        channel.send("alice@example.com", new Notification("[HIGH] Disaster: Quake", "Details"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("Alerts <alerts@example.com>");
        assertThat(message.getTo()).containsExactly("alice@example.com");
        assertThat(message.getSubject()).isEqualTo("[HIGH] Disaster: Quake");
        assertThat(message.getText()).isEqualTo("Details");
        assertThat(channel.type()).isEqualTo(ChannelType.EMAIL);
    }

    @Test
    void smtpFailureBecomesDeliveryExceptionSoTheDispatcherRetries() {
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> channel.send("alice@example.com", new Notification("s", "b")))
                .isInstanceOf(DeliveryException.class)
                .hasMessageContaining("Connection refused");
    }
}
