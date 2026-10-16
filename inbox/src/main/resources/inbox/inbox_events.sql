-- inbox 모듈을 쓰는 서비스는 이 테이블을 자기 Flyway 마이그레이션으로 만든다.
CREATE TABLE inbox_events
(
    id              BIGINT       GENERATED ALWAYS AS IDENTITY,
    event_id        VARCHAR(36)  NOT NULL,
    topic           VARCHAR(200) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    attempts        INT          NOT NULL,
    next_attempt_at TIMESTAMP(6) NOT NULL,
    last_error      VARCHAR(500) NULL,
    received_at     TIMESTAMP(6) NOT NULL,
    processed_at    TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_inbox_events_event_id UNIQUE (event_id)
);
CREATE INDEX idx_inbox_events_status_next_attempt ON inbox_events (status, next_attempt_at);
