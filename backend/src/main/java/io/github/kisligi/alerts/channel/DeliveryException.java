package io.github.kisligi.alerts.channel;

/** A channel could not deliver a notification. The dispatcher retries and logs the message. */
public class DeliveryException extends RuntimeException {

    public DeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
