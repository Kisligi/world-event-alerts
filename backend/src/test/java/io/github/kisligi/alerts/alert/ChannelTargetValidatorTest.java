package io.github.kisligi.alerts.alert;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kisligi.alerts.common.ChannelType;
import io.github.kisligi.alerts.common.InvalidRequestException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ChannelTargetValidatorTest {

    private final ChannelTargetValidator validator = new ChannelTargetValidator();

    @ParameterizedTest
    @ValueSource(strings = {"alice@example.com", "first.last+tag@mail.example.org"})
    void acceptsValidEmailAddresses(String address) {
        assertThatCode(() -> validator.validate(new ChannelTarget(ChannelType.EMAIL, address)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"alice", "alice@", "alice@example", "a b@example.com", "https://x.y"})
    void rejectsInvalidEmailAddresses(String address) {
        assertThatThrownBy(() -> validator.validate(new ChannelTarget(ChannelType.EMAIL, address)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("EMAIL");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://hooks.slack.com/services/T000/B000/XXXX",
            "http://slack-mock:8080/services/demo",
            "HTTPS://hooks.slack.com/services/T000/B000/XXXX"})
    void acceptsHttpWebhookUrls(String address) {
        assertThatCode(() -> validator.validate(new ChannelTarget(ChannelType.SLACK, address)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"alice@example.com", "ftp://example.com/hook", "not a url", "https://",
            "http:///no-host"})
    void rejectsNonHttpWebhookUrls(String address) {
        assertThatThrownBy(() -> validator.validate(new ChannelTarget(ChannelType.SLACK, address)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("SLACK");
    }
}
