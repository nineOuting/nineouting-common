# Changelog

## [0.10.0](https://github.com/nineOuting/nineouting-common/compare/v0.9.0...v0.10.0) (2026-10-29)


### Features

* 서비스 간 조회에 타임아웃·재시도 ([a28bbbf](https://github.com/nineOuting/nineouting-common/commit/a28bbbf8d9d3be45c79e8caa0d78b3aca43424b6))
* 서비스 간 조회에 타임아웃·재시도 ([#49](https://github.com/nineOuting/nineouting-common/issues/49)) ([4fae3c9](https://github.com/nineOuting/nineouting-common/commit/4fae3c9b4553d0c9d9e3e409cac13cb7ba2576f1))

## [0.9.0](https://github.com/nineOuting/nineouting-common/compare/v0.8.0...v0.9.0) (2026-10-28)


### Features

* 워크스페이스별 프로필 조회, 삭제된 요구사항 레거시 표시 ([#45](https://github.com/nineOuting/nineouting-common/issues/45)) ([bb1ba58](https://github.com/nineOuting/nineouting-common/commit/bb1ba586a45cd6d7d20b5391caeb0d778ed5d273))

## [0.8.0](https://github.com/nineOuting/nineouting-common/compare/v0.7.0...v0.8.0) (2026-10-28)


### Features

* SNS로 보내고 SQS로 받는 부품 추가 (Kafka 대신) ([0fb7bf9](https://github.com/nineOuting/nineouting-common/commit/0fb7bf95384932622b125769bd77b61fd1cd0753))
* SNS로 보내고 SQS로 받는 부품 추가 (Kafka 대신) ([1190daf](https://github.com/nineOuting/nineouting-common/commit/1190daf5642f0025fc62d5a1e0ea10627522e5e7))

## [0.7.0](https://github.com/nineOuting/nineouting-common/compare/v0.6.0...v0.7.0) (2026-10-26)


### Features

* 이메일 초대 이벤트 추가 (REQ-127) ([2280c4f](https://github.com/nineOuting/nineouting-common/commit/2280c4fe57451220fab653cd95dffffc797ee924))
* 회의 마무리 이벤트에 장소, 확정 후 수정 이벤트 추가 (REQ-055, 111) ([41bb09b](https://github.com/nineOuting/nineouting-common/commit/41bb09bf1154850e3e73c19077d0febafabc7139))
* 회의 장소·확정 후 수정·이메일 초대 이벤트 (REQ-055, 111, 127) ([f38bab1](https://github.com/nineOuting/nineouting-common/commit/f38bab1c02551c30fc01a8584299a5509da0e995))

## [0.6.0](https://github.com/nineOuting/nineouting-common/compare/v0.5.0...v0.6.0) (2026-10-20)


### ⚠ BREAKING CHANGES

* PasswordResetRequested의 resetLink 필드가 code로 바뀐다. user-service와 notification-service를 함께 올려야 한다.

### Features

* 비밀번호 찾기 이벤트를 재설정 링크에서 인증 번호로 변경 ([8207e4f](https://github.com/nineOuting/nineouting-common/commit/8207e4f513ec070045239a8acfaf7d62df5ca0b3))

## [0.5.0](https://github.com/nineOuting/nineouting-common/compare/v0.4.0...v0.5.0) (2026-10-15)


### Features

* api-docs 모듈 추가 (Swagger UI, OpenAPI) ([c97dde8](https://github.com/nineOuting/nineouting-common/commit/c97dde8eab7f82a0cfb47c9aeda336874ae60f0c))
* api-docs 모듈 추가 (Swagger UI, OpenAPI) ([c6a8708](https://github.com/nineOuting/nineouting-common/commit/c6a8708ab818d76fcf02d502b3266f5a2beb5acd))

## [0.4.0](https://github.com/nineOuting/nineouting-common/compare/v0.3.0...v0.4.0) (2026-10-14)


### Features

* client-common 모듈 추가 (사용자·폴더 권한 조회) ([6462672](https://github.com/nineOuting/nineouting-common/commit/6462672399dddfa89b281c77a85e91aa21f160f2))
* client-common 모듈 추가 (사용자·폴더 권한 조회) ([8f39e4e](https://github.com/nineOuting/nineouting-common/commit/8f39e4e9756504b5d26f1765aa1b402f96287817))
* 회의 마무리·회의록 검토 요청·확정 이벤트 추가 ([2703612](https://github.com/nineOuting/nineouting-common/commit/2703612a523a22e18289ddee6c0eda38456639c0))
* 회의 마무리·회의록 검토 요청·확정 이벤트 추가 ([209dfe6](https://github.com/nineOuting/nineouting-common/commit/209dfe6cd802153528ecbf36829a7b554b1e024d))

## [0.3.0](https://github.com/nineOuting/nineouting-common/compare/v0.2.0...v0.3.0) (2026-10-14)


### Features

* inbox 모듈 추가, outbox는 보낸 이벤트를 바로 삭제 ([c225f2b](https://github.com/nineOuting/nineouting-common/commit/c225f2b0c9fc354191f6763912de7663570ccf1b))
* inbox 모듈 추가, outbox는 보낸 이벤트를 바로 삭제 ([0083be4](https://github.com/nineOuting/nineouting-common/commit/0083be4ca92e1b7d6c133b80eee8271fa174bf61))
* meeting 이벤트 추가 (회의 생성·수정·취소·폴더 이동) ([bba6615](https://github.com/nineOuting/nineouting-common/commit/bba6615c8c1ff8f7238f3c42c4922fec1e294172))
* meeting 이벤트 추가 (회의 생성·수정·취소·폴더 이동) ([f19f93c](https://github.com/nineOuting/nineouting-common/commit/f19f93c1f1d05d2b8cf3931254fc9ea479f448a2))
* workspace 이벤트 추가 (참여 요청·승인, 폴더 삭제) ([175e959](https://github.com/nineOuting/nineouting-common/commit/175e95936b6d08226b0ddea5865329214efbf945))
* workspace 이벤트 추가 (참여 요청·승인, 폴더 삭제) ([42488a1](https://github.com/nineOuting/nineouting-common/commit/42488a1a75bccfdc06b2eb697ffd9014892daa98))

## [0.2.0](https://github.com/nineOuting/nineouting-common/compare/v0.1.0...v0.2.0) (2026-10-14)


### Features

* event-contract·outbox 모듈 추가 (v0.2.0) — dev 재반영 ([a993500](https://github.com/nineOuting/nineouting-common/commit/a993500418e682ee143534dd166ae61e25453843))


### Bug Fixes

* 게시 파일 버전 기록([#10](https://github.com/nineOuting/nineouting-common/issues/10)) stage 반영 ([7d0e7d7](https://github.com/nineOuting/nineouting-common/commit/7d0e7d722d2fca2a817eb4b276a0e1193e27639d))

## 0.1.0 (2026-10-14)


### Features

* observability 메트릭·로그·추적 기본 구성 추가 ([1d3a085](https://github.com/nineOuting/nineouting-common/commit/1d3a0856bb1609f934fc8a180031d35bccb00843))
* observability 메트릭·로그·추적 기본 구성 추가 ([4a6edcc](https://github.com/nineOuting/nineouting-common/commit/4a6edcc78e6af94cce68021211464841ad74df7e))
* security-common JWT 검증과 401·403 응답 추가 ([94259e9](https://github.com/nineOuting/nineouting-common/commit/94259e942633310e66541bab6af03f26fca2671c))
* security-common JWT 검증과 401·403 응답 추가 ([e1518f2](https://github.com/nineOuting/nineouting-common/commit/e1518f2c3a627121eee3b2d27f972e31708bdfd0))
* web-common 공통 응답 형식과 전역 예외 처리 추가 ([9891e0c](https://github.com/nineOuting/nineouting-common/commit/9891e0c7b1351a2a90cfd034179b7828943bfaa2))
* web-common 공통 응답 형식과 전역 예외 처리 추가 ([4012001](https://github.com/nineOuting/nineouting-common/commit/40120019cc79f56c2ba209248ac8ba7ea4dd9f7d))


### Bug Fixes

* 첫 릴리스 버전을 0.1.0으로 지정 ([7b5cd16](https://github.com/nineOuting/nineouting-common/commit/7b5cd167b21337bccadb75189fa612a569303266))
* 첫 릴리스 버전을 0.1.0으로 지정 ([1707f8f](https://github.com/nineOuting/nineouting-common/commit/1707f8f994de737eff5e2f68dcd2f60585777db5))
