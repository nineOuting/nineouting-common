-- outbox 모듈을 쓰는 서비스는 이 테이블을 자기 Flyway 마이그레이션으로 만든다 (예: V2__create_outbox_events.sql).
CREATE TABLE outbox_events
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    event_id        VARCHAR(36)  NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    destination     VARCHAR(200) NOT NULL,
    message_key     VARCHAR(100) NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    attempts        INT          NOT NULL,
    next_attempt_at DATETIME(6)  NOT NULL,
    last_error      VARCHAR(500) NULL,
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_outbox_events_event_id UNIQUE (event_id),
    INDEX idx_outbox_events_status_next_attempt (status, next_attempt_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
