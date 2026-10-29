package com.nineouting.backend.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nineouting.backend.common.web.error.BusinessException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** user·workspace-service 대신 JDK HTTP 서버를 띄워 실제 HTTP 호출과 응답 해석을 확인한다. */
class ClientTest {

    private static final String UNREACHABLE = "http://127.0.0.1:1";

    private final List<Integer> requestedBatchSizes = new ArrayList<>();
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void 사용자_정보를_조회하고_100명씩_나눠_요청한다() throws IOException {
        startServer();
        server.createContext("/internal/users", exchange -> {
            List<String> ids = Arrays.asList(exchange.getRequestURI().getQuery().replace("ids=", "").split("&")[0].split(","));
            requestedBatchSizes.add(ids.size());
            String data = ids.stream()
                    .map(id -> "{\"userId\":%s,\"name\":\"user%s\",\"email\":\"user%s@example.com\",\"profileImageUrl\":null}"
                            .formatted(id, id, id))
                    .collect(Collectors.joining(","));
            respond(exchange, 200, "{\"success\":true,\"data\":[" + data + "],\"message\":\"\"}");
        });
        UserClient client = new UserClient(restClient(baseUrl()));

        Map<Long, UserSummary> users = client.findUsers(LongStream.rangeClosed(1, 150).boxed().toList());

        assertThat(users).hasSize(150);
        assertThat(users.get(7L)).isEqualTo(new UserSummary(7L, "user7", "user7@example.com", null));
        assertThat(requestedBatchSizes).containsExactly(100, 50);
    }

    @Test
    void 워크스페이스를_주면_워크스페이스_프로필을_달라고_한다() throws IOException {
        startServer();
        List<String> queries = new ArrayList<>();
        server.createContext("/internal/users", exchange -> {
            queries.add(exchange.getRequestURI().getQuery());
            respond(exchange, 200, "{\"success\":true,\"data\":[{\"userId\":7,\"name\":\"김PM\",\"email\":\"user7@example.com\","
                    + "\"profileImageUrl\":null,\"affiliation\":\"기획팀\"}],\"message\":\"\"}");
        });
        UserClient client = new UserClient(restClient(baseUrl()));

        assertThat(client.findUsers(List.of(7L), 3L).get(7L))
                .isEqualTo(new UserSummary(7L, "김PM", "user7@example.com", null, "기획팀"));
        client.findUsers(List.of(7L));

        assertThat(queries).containsExactly("ids=7&workspaceId=3", "ids=7");
    }

    @Test
    void user_service에_연결할_수_없으면_503이다() {
        UserClient client = new UserClient(restClient(UNREACHABLE));

        assertThatThrownBy(() -> client.findUsers(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode.code").isEqualTo("USER_SERVICE_UNAVAILABLE");
    }

    @Test
    void 폴더_권한_멤버_목록을_해석하고_없는_폴더는_404다() throws IOException {
        startServer();
        server.createContext("/internal/folders/7/access", exchange -> respond(exchange, 200,
                "{\"success\":true,\"data\":{\"folderId\":7,\"workspaceId\":1,\"allowed\":true},\"message\":\"\"}"));
        server.createContext("/internal/folders/7/members", exchange -> respond(exchange, 200,
                "{\"success\":true,\"data\":[3,4],\"message\":\"\"}"));
        server.createContext("/internal/users/3/folders", exchange -> respond(exchange, 200,
                "{\"success\":true,\"data\":[{\"folderId\":7,\"workspaceId\":1,\"workspaceName\":\"팀\",\"parentId\":null,"
                        + "\"name\":\"기획\"}],\"message\":\"\"}"));
        server.createContext("/internal/folders/404/access", exchange -> respond(exchange, 404,
                "{\"success\":false,\"data\":null,\"message\":\"요청한 대상을 찾을 수 없습니다\",\"code\":\"NOT_FOUND\"}"));
        WorkspaceClient client = new WorkspaceClient(restClient(baseUrl()));

        assertThat(client.access(7L, 3L)).isEqualTo(new FolderAccess(7L, 1L, true));
        assertThat(client.folderMembers(7L)).containsExactlyInAnyOrder(3L, 4L);
        assertThat(client.accessibleFolders(3L)).containsExactly(new AccessibleFolder(7L, 1L, "팀", null, "기획"));
        assertThatThrownBy(() -> client.access(404L, 3L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void workspace_service에_연결할_수_없으면_503이다() {
        WorkspaceClient client = new WorkspaceClient(restClient(UNREACHABLE));

        assertThatThrownBy(() -> client.folderMembers(7L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode.code").isEqualTo("WORKSPACE_SERVICE_UNAVAILABLE");
    }

    @Test
    void 응답이_늦으면_한_번_더_부르고_또_늦으면_503이다() throws IOException {
        startServer();
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/internal/folders/7/members", exchange -> {
            if (calls.incrementAndGet() == 1) {
                sleep(1000);
            }
            respond(exchange, 200, "{\"success\":true,\"data\":[3],\"message\":\"\"}");
        });
        ClientProperties properties = new ClientProperties(null, baseUrl(), null, Duration.ofMillis(300));
        WorkspaceClient client = new WorkspaceClient(
                ClientAutoConfiguration.client(RestClient.builder(), properties).baseUrl(baseUrl()).build());

        assertThat(client.folderMembers(7L)).containsExactly(3L);
        assertThat(calls).hasValue(2);

        server.createContext("/internal/folders/8/members", exchange -> {
            calls.incrementAndGet();
            sleep(1000);
            respond(exchange, 200, "{\"success\":true,\"data\":[],\"message\":\"\"}");
        });
        assertThatThrownBy(() -> client.folderMembers(8L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode.code").isEqualTo("WORKSPACE_SERVICE_UNAVAILABLE");
        assertThat(calls).hasValue(4);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        // 늦은 응답을 기다리는 동안 다음 요청도 받는다
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static RestClient restClient(String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
