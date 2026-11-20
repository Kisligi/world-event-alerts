package io.github.kisligi.alerts.channel;

import io.github.kisligi.alerts.common.ChannelType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends notifications as plain-text email over SMTP (Mailpit in the demo setup). */
@Component
public class EmailChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailChannel(JavaMailSender mailSender, @Value("${alerts.channels.email.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public ChannelType type() {
        return ChannelType.EMAIL;
    }

    @Override
    public void send(String address, Notification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(address);
        message.setSubject(notification.subject());
        message.setText(notification.body());
        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new DeliveryException("SMTP send failed: " + e.getMessage(), e);
        }
    }
}
