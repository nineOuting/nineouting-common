package com.nineouting.backend.common.inbox;

/**
 * 서비스가 받은 이벤트를 처리하는 곳. 예외를 던지면 처리 중 바꾼 DB 내용은 되돌리고 나중에 다시 시도한다.
 * 같은 이벤트는 inbox가 한 번만 넘기지만, 처리 후 결과를 저장하기 전에 죽으면 다시 올 수 있으니
 * 가능하면 두 번 처리해도 결과가 같게 만든다.
 */
public interface InboxHandler {

    /** 이 처리기가 맡는 eventType (예: MemberJoinRequested.TYPE) */
    boolean supports(String eventType);

    void handle(InboxMessage message);
}
