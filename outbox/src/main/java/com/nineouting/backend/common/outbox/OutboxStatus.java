package com.nineouting.backend.common.outbox;

public enum OutboxStatus {
    /** 아직 보내지 않았거나, 실패해서 다시 보낼 차례를 기다리는 중 */
    PENDING,
    SENT,
    /** 정해진 횟수만큼 실패해서 더 이상 보내지 않는다. 운영자가 원인을 보고 다시 PENDING으로 돌린다 */
    FAILED
}
