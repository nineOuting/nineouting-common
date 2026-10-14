package com.nineouting.backend.common.client;

/** workspace-service /internal/folders/{folderId}/access 응답 */
public record FolderAccess(Long folderId, Long workspaceId, boolean allowed) {
}
