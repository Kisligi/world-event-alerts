-- Users and their alert rules (milestone M1).

CREATE TABLE app_user (
    id    BIGSERIAL    PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE alert_rule (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES app_user (id),
    name         VARCHAR(100) NOT NULL,
    min_severity VARCHAR(20)  NOT NULL,
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_alert_rule_user ON alert_rule (user_id);

-- Categories the rule listens to (NEWS, MARKET, DISASTER).
CREATE TABLE alert_rule_category (
    alert_rule_id BIGINT      NOT NULL REFERENCES alert_rule (id) ON DELETE CASCADE,
    category      VARCHAR(20) NOT NULL,
    PRIMARY KEY (alert_rule_id, category)
);

-- Optional keywords, stored lower-case; an event matches if its text contains any of them.
CREATE TABLE alert_rule_keyword (
    alert_rule_id BIGINT       NOT NULL REFERENCES alert_rule (id) ON DELETE CASCADE,
    keyword       VARCHAR(100) NOT NULL,
    PRIMARY KEY (alert_rule_id, keyword)
);

-- Where to notify: channel type + channel-specific address (email or webhook URL).
CREATE TABLE alert_rule_target (
    alert_rule_id BIGINT       NOT NULL REFERENCES alert_rule (id) ON DELETE CASCADE,
    channel_type  VARCHAR(20)  NOT NULL,
    address       VARCHAR(500) NOT NULL,
    PRIMARY KEY (alert_rule_id, channel_type, address)
);
