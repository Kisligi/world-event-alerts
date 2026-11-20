-- Notification delivery log and global channel switches (milestone M3).

-- One row per (event, alert rule, channel target): the delivery log.
-- It doubles as the de-duplication record: the unique key means a notification is
-- created at most once, however often the same match is announced.
CREATE TABLE delivery (
    id              BIGSERIAL     PRIMARY KEY,
    event_id        BIGINT        NOT NULL REFERENCES world_event (id) ON DELETE CASCADE,
    -- Kept as history if the rule is deleted later, hence SET NULL plus a name snapshot.
    alert_rule_id   BIGINT        REFERENCES alert_rule (id) ON DELETE SET NULL,
    alert_rule_name VARCHAR(100)  NOT NULL,
    user_id         BIGINT        NOT NULL REFERENCES app_user (id),
    channel_type    VARCHAR(20)   NOT NULL,
    address         VARCHAR(500)  NOT NULL,
    status          VARCHAR(20)   NOT NULL,      -- PENDING, SENT, FAILED, SKIPPED
    attempts        INT           NOT NULL DEFAULT 0,
    last_error      VARCHAR(1000),
    subject         VARCHAR(400)  NOT NULL,
    body            TEXT          NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_delivery_event_rule_target UNIQUE (event_id, alert_rule_id, channel_type, address)
);

CREATE INDEX idx_delivery_user_created ON delivery (user_id, created_at DESC);
CREATE INDEX idx_delivery_created ON delivery (created_at DESC);

-- Admin switch per channel type. A disabled channel records deliveries as SKIPPED.
CREATE TABLE channel_setting (
    channel_type VARCHAR(20) PRIMARY KEY,
    enabled      BOOLEAN     NOT NULL
);

INSERT INTO channel_setting (channel_type, enabled) VALUES ('EMAIL', TRUE), ('SLACK', TRUE);
