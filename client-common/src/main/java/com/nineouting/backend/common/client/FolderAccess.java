package com.nineouting.backend.common.client;

/**
 * workspace-service /internal/folders/{folderId}/access 응답.
 *
 * @param owner 폴더 소유자인지. 소유자가 없는 기본 폴더는 워크스페이스 소유자 (REQ-160 회의록 삭제)
 */
public record FolderAccess(Long folderId, Long workspaceId, boolean allowed, boolean owner) {

    /** owner를 모르는 예전 호출용. 소유자가 아닌 것으로 본다 */
    public FolderAccess(Long folderId, Long workspaceId, boolean allowed) {
        this(folderId, workspaceId, allowed, false);
    }
}
