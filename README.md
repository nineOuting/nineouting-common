# nineouting-common

nineouting 백엔드 서비스들이 함께 쓰는 공통 모듈.
서비스는 필요한 모듈만 골라 의존한다. 도메인 Entity·Repository·비즈니스 로직은 넣지 않는다.

> 배포: GitHub Packages (Maven) · `com.nineouting.backend:<모듈>:<버전>`
> 기획·규칙 문서: [nineouting-docs](https://github.com/nineOuting/nineouting-docs)

## 모듈

| 모듈 | 내용 | 상태 |
|------|------|------|
| `web-common` | 공통 응답 `{ success, data, message }`, 공통 예외·에러 코드, 전역 예외 처리 | 준비 중 (v0.1.0) |
| `security-common` | JWT 검증 (공개키 JWKS 방식). 발급자 `user-service`는 쓰지 않는다 | 준비 중 (v0.1.0) |
| `observability` | JSON 로그 형식, trace ID 로그 출력, 분산 추적 설정 | 준비 중 (v0.1.0) |
| `event-contract` | 이벤트 봉투(`EventEnvelope`)와 이벤트별 내용. 런타임 의존성 없음 | v0.2.0 |
| `outbox` | Transactional Outbox: DB 트랜잭션과 함께 이벤트를 기록하고 Kafka로 전송 | v0.2.0 |
| `client-common` | 서비스 간 REST 호출 + 재시도·차단 | 서비스 간 호출이 처음 생길 때 추가 |

## 서비스에서 쓰는 방법

`build.gradle`:
```groovy
repositories {
    mavenCentral()
    maven {
        url = uri('https://maven.pkg.github.com/nineOuting/nineouting-common')
        credentials {
            username = System.getenv('GITHUB_ACTOR') ?: findProperty('gpr.user')
            password = System.getenv('GPR_TOKEN') ?: findProperty('gpr.key')
        }
    }
}

dependencies {
    implementation 'com.nineouting.backend:web-common:0.1.0'
}
```

- GitHub Packages(Maven)는 public 레포여도 **읽을 때 토큰이 필요하다.**
- 로컬: `~/.gradle/gradle.properties`에 아래를 넣는다.
  ```properties
  gpr.user=<GitHub 아이디>
  gpr.key=<read:packages 권한 토큰>
  ```
- CI: 조직 시크릿 `GPR_TOKEN`(read:packages 토큰)을 Gradle 단계 환경 변수로 넣는다.
- 항상 정확한 버전으로 의존한다. `SNAPSHOT`은 쓰지 않는다.

### security-common

user-service가 RS256으로 발급한 access 토큰을 공개키(JWKS)로 검증한다. 토큰에는 사용자 ID(`sub`)만 들어 있고,
워크스페이스·회의 역할은 각 서비스가 조회한다.

`application.yml`:
```yaml
nineouting:
  security:
    jwt:
      jwks-uri: ${JWT_JWKS_URI}   # 예: http://user-service:8080/.well-known/jwks.json
      # issuer: nineouting-user-service (기본값)
```

어떤 주소를 막을지는 서비스마다 다르므로 `SecurityFilterChain`은 서비스가 만든다:
```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
        JsonAuthenticationEntryPoint entryPoint, JsonAccessDeniedHandler accessDeniedHandler) throws Exception {
    return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health/**").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
            .build();
}
```

Controller에서 로그인 사용자 꺼내기:
```java
@GetMapping("/api/meetings")
public ApiResponse<List<MeetingResponse>> list(@CurrentUser AuthenticatedUser user) { ... }
```

- 401: 토큰 없음·잘못됨 → `UNAUTHORIZED`, 만료 → `TOKEN_EXPIRED` (프론트는 refresh 토큰으로 재발급)
- 403: `FORBIDDEN`. 서비스 권한(회의록 작성자만 수정 등)은 서비스가 `BusinessException(CommonErrorCode.FORBIDDEN)`으로 처리한다.

## 릴리스

[release-please](https://github.com/googleapis/release-please)가 자동으로 처리한다. 사람이 하는 일은 **Release PR 머지 한 번**이다.

1. `main`에 Conventional Commits(`feat:`, `fix:`, `feat!:`)가 쌓인다.
2. release-please가 Release PR(버전 + `CHANGELOG.md`)을 만든다.
3. Release PR을 머지하면 `vX.Y.Z` 태그가 만들어지고, 빌드가 통과하면 모든 모듈이 GitHub Packages에 게시된다.

- 버전 규칙: 0.x 동안 `feat` → minor, `fix` → patch, breaking → minor
- 수동 게시(비상용): Actions → **Publish (manual)** → 버전 입력

## 로컬 빌드

JDK 21이 필요하다.

```bash
./gradlew build                # 컴파일 + 테스트
./gradlew publishToMavenLocal  # ~/.m2에 게시 (서비스에서 로컬로 확인할 때)
```

## observability 설정

모든 서비스가 같은 방식으로 메트릭·로그·추적을 남기도록 의존성과 기본 설정을 묶은 모듈이다.
Java 코드는 없고, 이 모듈을 추가하면 아래가 함께 들어온다.

- Actuator + Prometheus 메트릭 (`/actuator/prometheus`)
- 쿠버네티스 헬스 체크 (`/actuator/health/liveness`, `/actuator/health/readiness`)
- OpenTelemetry 분산 추적 (OTLP로 전송, 서비스 간 요청에 trace 정보 전파)
- 로그에 traceId·spanId 출력, `local` 외 프로필에서는 JSON(logstash) 로그

`application.yml`에서 기본 설정을 불러온다 (같은 키를 쓰면 서비스 값이 우선):
```yaml
spring:
  application:
    name: user-service
  config:
    import: classpath:observability/defaults.yml
```

| 환경 변수 | 기본값 | 설명 |
|---|---|---|
| `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | `http://localhost:4318/v1/traces` | 추적 데이터를 받을 수집기 주소 |
| `SPRING_PROFILES_ACTIVE` | — | `local`이면 사람이 읽는 로그 + 추적 전송 끔 |

- 엔드포인트 경로·수집 방식은 모니터링 담당과 맞춘 값이다. 바꿀 때는 담당자와 먼저 협의한다.
- `defaults.yml`의 설정 이름은 테스트(`DefaultsPropertyNamesTest`)가 실제 Spring Boot 설정과 대조한다.

## outbox 설정

데이터 변경과 이벤트 기록을 **같은 DB 트랜잭션**에 저장하고, 별도 작업(릴레이)이 Kafka로 보낸다.
Kafka가 잠시 멈춰도 이벤트가 사라지지 않고, 데이터는 저장됐는데 이벤트만 빠지는 일이 없다.

1. 의존성: `outbox` + `spring-boot-starter-data-jpa` + `spring-boot-starter-kafka`
2. 테이블: [`outbox/outbox_events.sql`](outbox/src/main/resources/outbox/outbox_events.sql)을 서비스의 Flyway 마이그레이션으로 추가
3. 사용 (반드시 `@Transactional` 안에서):
   ```java
   @Transactional
   public void request(String email) {
       // ... 데이터 변경
       outboxEventWriter.append(PasswordResetRequested.TOPIC, email, PasswordResetRequested.TYPE,
               new PasswordResetRequested(email, link));
   }
   ```

- 엔티티·저장소는 자동 설정이 등록하므로 서비스에 `@EntityScan`을 따로 쓰지 않는다.
- 릴레이는 1초마다 최대 100건을 보낸다. 실패하면 1초, 2초, 4초 … 최대 5분 간격으로 다시 보내고, 10번 실패하면 `FAILED`로 남긴다.
- 서비스를 여러 개 띄워도 `SELECT … FOR UPDATE SKIP LOCKED`로 같은 이벤트를 동시에 보내지 않는다.
- 같은 이벤트가 두 번 갈 수 있다(at-least-once). **받는 쪽은 `eventId`로 중복을 거른다.**
- 봉투에 W3C `traceparent`가 들어가 받는 쪽에서 추적을 이어 갈 수 있다.
- 보낸 이벤트는 7일 뒤 지운다(매일 04:00). `FAILED`는 원인 확인을 위해 남긴다.

| 설정 | 기본값 |
|---|---|
| `nineouting.outbox.enabled` | `true` |
| `nineouting.outbox.source` | `spring.application.name` |
| `nineouting.outbox.relay-interval` | `1s` |
| `nineouting.outbox.batch-size` | `100` |
| `nineouting.outbox.max-attempts` | `10` |
| `nineouting.outbox.max-retry-delay` | `5m` |
| `nineouting.outbox.retention` | `7d` |
