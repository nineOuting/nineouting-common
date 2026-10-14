package com.nineouting.backend.common.inbox;

public enum InboxStatus {
    /** 받았고, 처리할 차례를 기다리는 중 (실패 후 재시도 대기 포함) */
    RECEIVED,
    /** 처리 끝. 같은 이벤트가 다시 오면 이 기록으로 걸러낸다 */
    PROCESSED,
    /** 정해진 횟수만큼 처리에 실패했다. 운영자가 원인을 보고 다시 RECEIVED로 돌린다 */
    FAILED
}
