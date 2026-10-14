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
| `event-contract` | Kafka 이벤트 봉투와 이벤트별 내용 | workspace-service 개발 시 추가 |
| `outbox` | Transactional Outbox (Kafka·REST 전송) | workspace·meeting·minutes 개발 시 추가 |
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
