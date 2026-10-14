package com.nineouting.backend.common.outbox;

/**
 * 보낸 이벤트는 행을 바로 지우므로 "보냄" 상태는 없다.
 */
public enum OutboxStatus {
    /** 아직 보내지 않았거나, 실패해서 다시 보낼 차례를 기다리는 중 */
    PENDING,
    /** 정해진 횟수만큼 실패해서 더 이상 보내지 않는다. 운영자가 원인을 보고 다시 PENDING으로 돌린다 */
    FAILED
}
