package com.nineouting.backend.common.client;

import com.nineouting.backend.common.web.error.BusinessException;
import com.nineouting.backend.common.web.response.ApiResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 사용자 이름·이메일은 user-service가 가지고 있으므로 필요할 때 조회한다 (다른 서비스는 사용자 ID만 저장).
 * 연결할 수 없으면 503 USER_SERVICE_UNAVAILABLE.
 */
public class UserClient {

    /** user-service가 한 번에 받는 최대 개수 */
    private static final int BATCH_SIZE = 100;
    private static final ParameterizedTypeReference<ApiResponse<List<UserSummary>>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;

    public UserClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /** 계정 프로필. 없는 사용자는 결과에서 빠진다. 메일 주소처럼 워크스페이스와 상관없는 값에 쓴다 */
    public Map<Long, UserSummary> findUsers(Collection<Long> userIds) {
        return findUsers(userIds, null);
    }

    /**
     * 워크스페이스 안에서 보여 줄 이름·사진·소속 (REQ-114·115·118). 그 워크스페이스에서 따로 정하지 않았으면 계정 프로필.
     * 회의 참석자·멤버 목록처럼 화면에 사람을 보여 줄 때 쓴다. 없는 사용자는 결과에서 빠진다.
     */
    public Map<Long, UserSummary> findUsers(Collection<Long> userIds, Long workspaceId) {
        List<Long> ids = userIds.stream().distinct().toList();
        Map<Long, UserSummary> result = new HashMap<>();
        for (int from = 0; from < ids.size(); from += BATCH_SIZE) {
            fetch(ids.subList(from, Math.min(from + BATCH_SIZE, ids.size())), workspaceId)
                    .forEach(user -> result.put(user.userId(), user));
        }
        return result;
    }

    private List<UserSummary> fetch(List<Long> ids, Long workspaceId) {
        try {
            ApiResponse<List<UserSummary>> response = restClient.get()
                    .uri(uri -> {
                        uri.path("/internal/users")
                                .queryParam("ids", ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
                        if (workspaceId != null) {
                            uri.queryParam("workspaceId", workspaceId);
                        }
                        return uri.build();
                    })
                    .retrieve()
                    .body(RESPONSE_TYPE);
            return response == null || response.data() == null ? new ArrayList<>() : response.data();
        } catch (RestClientException e) {
            throw new BusinessException(ClientErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }
}
