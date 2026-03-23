CREATE TABLE outbox_messages
(
    id          UUID                        NOT NULL,
    event_type  VARCHAR(255)                NOT NULL,
    payload     JSONB                       NOT NULL,
    status      VARCHAR(255)                NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    retry_count INT                         NULL,
    CONSTRAINT pk_outbox_messages PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_status_created ON outbox_messages (status, created_at);