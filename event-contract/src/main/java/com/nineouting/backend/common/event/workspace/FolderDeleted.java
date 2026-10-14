package com.nineouting.backend.common.event.workspace;

import java.util.List;

/**
 * workspace-service → meeting·minutes-service: 폴더(하위 폴더 포함)가 삭제됐다.
 * 받는 쪽은 이 폴더들의 회의·회의록을 지운다 (REQ-114). minutes는 ai-service 챗봇 복사본도 지운다.
 *
 * @param folderIds 삭제된 폴더와 그 하위 폴더 전부
 */
public record FolderDeleted(Long workspaceId, List<Long> folderIds) {

    public static final String TYPE = "FolderDeleted";
    public static final String TOPIC = "workspace.folder-deleted";
}
