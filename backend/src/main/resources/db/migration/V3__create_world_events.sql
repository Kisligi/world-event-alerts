-- World events received from event sources (milestone M2).

CREATE TABLE world_event (
    id          BIGSERIAL     PRIMARY KEY,
    source      VARCHAR(30)   NOT NULL,           -- SIMULATOR, USGS, ADMIN, ...
    external_id VARCHAR(100),                     -- the source's own id; NULL for manual events
    category    VARCHAR(20)   NOT NULL,
    severity    VARCHAR(20)   NOT NULL,
    title       VARCHAR(300)  NOT NULL,
    description VARCHAR(2000),
    url         VARCHAR(500),
    occurred_at TIMESTAMPTZ   NOT NULL,
    received_at TIMESTAMPTZ   NOT NULL,
    -- The same event from the same source is stored only once.
    CONSTRAINT uq_world_event_source_external_id UNIQUE (source, external_id)
);

CREATE INDEX idx_world_event_received_at ON world_event (received_at DESC);
