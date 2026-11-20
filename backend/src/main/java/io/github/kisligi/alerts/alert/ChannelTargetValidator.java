package io.github.kisligi.alerts.alert;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import io.github.kisligi.alerts.common.InvalidRequestException;
import org.springframework.stereotype.Component;

/**
 * Checks that a target's address fits its channel type.
 *
 * <p>Slack webhook URLs may use plain {@code http}, because in the demo setup they point
 * to a local Slack stand-in. Real Slack webhooks are {@code https://hooks.slack.com/...}.
 */
@Component
public class ChannelTargetValidator {

    /** Deliberately simple: one "@", no spaces, a dot in the domain part. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public void validate(ChannelTarget target) {
        switch (target.type()) {
            case EMAIL -> {
                if (!EMAIL.matcher(target.address()).matches()) {
                    throw new InvalidRequestException(
                            "EMAIL target is not a valid email address: " + target.address());
                }
            }
            case SLACK -> {
                if (!isHttpUrl(target.address())) {
                    throw new InvalidRequestException(
                            "SLACK target must be an http(s) webhook URL: " + target.address());
                }
            }
        }
    }

    private static boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
