package com.nineouting.backend.common.outbox;

/**
 * 기록된 이벤트를 실제로 내보낸다. 기본은 SNS({@link SnsOutboxSender}).
 * 실패하면 예외를 던진다 — 릴레이가 나중에 다시 보낸다.
 */
@FunctionalInterface
public interface OutboxSender {

    void send(String destination, String messageKey, String payload) throws Exception;
}
