# 과제 체크리스트 정리

> 기준일: `2026-04-27`  
> 판정 기준: 현재 저장소에 남아 있는 코드, 문서, 테스트, 워크플로우 기준  
> 주의: AWS 콘솔이나 외부 산출물 기반 항목은 제출 시 별도 캡처/링크를 붙이면 더 안전하다

## 상태 기준

- `충족`: 저장소 안에서 구현과 근거를 직접 확인할 수 있는 항목
- `부분 충족`: 구현은 있으나 과제 문구와 정확히 일치하지 않거나, 저장소 밖 증빙이 추가로 필요한 항목
- `미충족`: 현재 저장소 기준으로 구현/증빙이 부족한 항목

## 1. 필수 기능 가이드

### 1-1. 동시성 제어

| 요구사항 | 상태 | 구현/근거 | 비고 |
| --- | --- | --- | --- |
| 순간적으로 많은 요청이 쏟아지는 비즈니스 기획 및 개발 | 충족 | ArtistPost 좋아요/댓글 burst 경로, Jelly 결제 차감 비교 | [`docs/artistcontent-concurrency-comparison.md`](artistcontent-concurrency-comparison.md), [`docs/concurrency-comparison.md`](concurrency-comparison.md) |
| 동시성 이슈를 검증할 수 있는 테스트 코드 작성 | 충족 | `no lock` 실패 -> `Lettuce v1` 성공 비교 테스트 | [`ArtistPostLikeVersionComparisonIntegrationTest.java`](../src/test/java/com/example/infinite/domain/artistcontent/interaction/service/artistpostlike/ArtistPostLikeVersionComparisonIntegrationTest.java) |
| Redis 를 이용한 Lettuce Lock 구현 | 충족 | `SET NX + TTL + UUID + Lua unlock` 구조 구현 | [`LettuceLockService.java`](../src/main/java/com/example/infinite/global/lock/lettuce/LettuceLockService.java), [`ArtistPostLikeLettuceV1Service.java`](../src/main/java/com/example/infinite/domain/artistcontent/interaction/service/artistpostlike/ArtistPostLikeLettuceV1Service.java) |

### 1-2. 캐싱을 이용한 성능 개선

| 요구사항 | 상태 | 구현/근거 | 비고 |
| --- | --- | --- | --- |
| 검색어 및 검색 기능 기획 및 개발 | 충족 | `LIKE` 검색, 페이징, 인기 검색어 ZSet | [`ArtistService.java`](../src/main/java/com/example/infinite/domain/member/artist/service/ArtistService.java), [`docs/artist-search-caching.md`](artist-search-caching.md) |
| 인기 검색어 API 제공 | 충족 | Redis ZSet + 동일 사용자 중복 집계 방지 | [`docs/artist-search-caching.md`](artist-search-caching.md) |
| 검색 API에 Local Memory Cache 적용한 v2 API 추가 | 충족 | `v1 원본`, `v2 Caffeine` 구현 | [`CacheConfig.java`](../src/main/java/com/example/infinite/global/common/config/CacheConfig.java), [`ArtistSearchCachingIntegrationTest.java`](../src/test/java/com/example/infinite/domain/member/artist/service/ArtistSearchCachingIntegrationTest.java) |
| TTL / maximumSize / `@Cacheable` 기반 캐싱 적용 | 충족 | `artistSearchV2` TTL 10분, maximumSize 1000 | [`member-artistcontent-design-rationale.md`](member-artistcontent-design-rationale.md), [`CacheConfig.java`](../src/main/java/com/example/infinite/global/common/config/CacheConfig.java) |

## 2. 도전 기능 가이드

### 2-1. 동시성 제어

| 요구사항 | 상태 | 구현/근거 | 비고 |
| --- | --- | --- | --- |
| Lock 을 AOP 방식으로 적용하도록 리팩토링 | 충족 | `@RedisLock` + `Aspect` + 동적 키 | [`RedisLock.java`](../src/main/java/com/example/infinite/global/lock/RedisLock.java), [`RedisLockAspect.java`](../src/main/java/com/example/infinite/global/lock/RedisLockAspect.java) |
| 낙관적 락(`@Version`) 구현 + 재시도 로직 | 부분 충족 | `@Version` 비교/분석은 존재 | [`docs/concurrency-comparison.md`](concurrency-comparison.md), [`JellyConcurrencyTest.java`](../src/test/java/com/example/infinite/domain/payment/service/JellyConcurrencyTest.java) ; 자동 재시도 로직은 현재 저장소 기준 증빙이 약하다 |
| Redis 대신 MySQL Lock 구현 | 충족 | 결제/젤리에서 `PESSIMISTIC_WRITE` 기반 비교 | [`JellyService.java`](../src/main/java/com/example/infinite/domain/payment/service/JellyService.java), [`docs/concurrency-comparison.md`](concurrency-comparison.md) |
| `Redisson` 을 이용한 Redis Lock 개발 | 충족 | Redisson 락 서비스와 AOP 경로 구현 | [`RedissonLockService.java`](../src/main/java/com/example/infinite/global/lock/redisson/RedissonLockService.java) |
| 비관적 / 낙관적 / 분산 락 비교 분석 및 선택 근거 문서화 | 충족 | 비교 문서와 테스트 존재 | [`docs/concurrency-comparison.md`](concurrency-comparison.md), [`docs/artistcontent-concurrency-comparison.md`](artistcontent-concurrency-comparison.md) |

### 2-2. 캐싱을 이용한 성능 개선

| 요구사항 | 상태 | 구현/근거 | 비고 |
| --- | --- | --- | --- |
| v2 검색 API 를 Redis Remote Cache 로 수정 | 충족 | 로컬 캐시 비교 축을 유지하면서 Redis remote cache 경로까지 확장 구현 | [`docs/artist-search-caching.md`](artist-search-caching.md), [`RedisCacheConfig.java`](../src/main/java/com/example/infinite/global/common/config/RedisCacheConfig.java) |
| 검색 대상 테이블에 5만 건 이상 Dummy 데이터 적재 | 충족 | `artists` 50,000건 기준 벤치마크 | [`docs/artist-search-caching.md`](artist-search-caching.md) |
| v1, v2 API 성능 테스트 및 보고서 작성 | 충족 | 벤치마크 테스트 코드와 성능 보고서 문서 존재 | [`ArtistSearchCachingBenchmarkIntegrationTest.java`](../src/test/java/com/example/infinite/domain/member/artist/service/ArtistSearchCachingBenchmarkIntegrationTest.java), [`docs/artist-search-caching.md`](artist-search-caching.md) |
| Cache Eviction 으로 캐시 동기화 문제 해결 | 충족 | 상세는 `@CacheEvict`, 검색은 의도적으로 `stale + TTL`을 택했고 그 근거를 문서화했다 | [`docs/artist-search-caching.md`](artist-search-caching.md), [`member-artistcontent-design-rationale.md`](member-artistcontent-design-rationale.md) |

### 2-3. 최적화(Indexing)

| 요구사항 | 상태 | 구현/근거 | 비고 |
| --- | --- | --- | --- |
| 성능 개선 대상 쿼리 선정 및 병목 분석 | 충족 | EXPLAIN 기반 before/after 분석 | [`docs/index-analysis.md`](index-analysis.md), [`docs/member-artistcontent-index-analysis.md`](member-artistcontent-index-analysis.md) |
| 적절한 인덱스 설계 및 적용 | 충족 | 복합 인덱스 설계와 엔티티 반영 | [`docs/index-analysis.md`](index-analysis.md), [`docs/member-artistcontent-index-analysis.md`](member-artistcontent-index-analysis.md) |
| 인덱스 적용 전/후 성능 비교 및 분석 | 충족 | EXPLAIN / EXPLAIN ANALYZE 비교 | [`scripts/measure-member-artistcontent-index-benchmark.ps1`](../scripts/measure-member-artistcontent-index-benchmark.ps1) |
| 5만 건 이상 데이터에서 인덱스 효과 검증 | 충족 | `dm_messages`, `raffle_entries`, `live_chat_messages` 50,000건+, `member+artistcontent` 100,000건 실험 | [`docs/index-analysis.md`](index-analysis.md), [`scripts/measure-member-artistcontent-index-benchmark.ps1`](../scripts/measure-member-artistcontent-index-benchmark.ps1) |

### 2-4. 실시간 채팅

| 요구사항 | 상태 | 구현/근거 | 비고 |
| --- | --- | --- | --- |
| WebSocket 기반 실시간 양방향 통신 + STOMP | 충족 | `/ws-stomp`, `/pub`, `/sub` 구조 | [`WebSocketConfig.java`](../src/main/java/com/example/infinite/global/common/config/WebSocketConfig.java) |
| 채팅 도메인 설계 및 메시지 영속화 | 충족 | DM / Live 메시지 엔티티, 저장, 커서 조회 API | [`docs/api-overview.md`](api-overview.md), [`DmMessage.java`](../src/main/java/com/example/infinite/domain/dm/entity/DmMessage.java), [`LiveChatMessage.java`](../src/main/java/com/example/infinite/domain/realtimelive/entity/LiveChatMessage.java) |
| 채팅방 관리 및 입장/퇴장 + JWT 기반 식별 + 재연결 전략 | 부분 충족 | 채팅방 목록/메시지 조회와 STOMP JWT 인증은 존재 | [`DmController.java`](../src/main/java/com/example/infinite/domain/dm/controller/DmController.java), [`StompAuthChannelInterceptor.java`](../src/main/java/com/example/infinite/global/auth/StompAuthChannelInterceptor.java) ; 시스템 입장/퇴장 메시지와 reconnect 복구 전략은 저장소 기준 증빙이 약하다 |
| Redis Pub/Sub 기반 다중 서버 브로드캐스팅 | 충족 | 라이브 채팅 Redis Pub/Sub 브로드캐스팅 구현 | [`RedisLiveChatConfig.java`](../src/main/java/com/example/infinite/domain/realtimelive/config/RedisLiveChatConfig.java), [`RedisLiveChatSubscriber.java`](../src/main/java/com/example/infinite/domain/realtimelive/service/RedisLiveChatSubscriber.java) |

### 2-5. 배포와 CI/CD

| 요구사항 | 상태 | 구현/근거 | 비고 |
| --- | --- | --- | --- |
| Docker 컨테이너화 | 충족 | `Dockerfile`, `docker-compose.yml` 존재 | [`Dockerfile`](../Dockerfile), [`docker-compose.yml`](../docker-compose.yml) |
| AWS 인프라 구성 | 부분 충족 | ECR/EC2/ASG 기반 배포 흐름은 확인 가능 | [`.github/workflows/ci-cd.yml`](../.github/workflows/ci-cd.yml) ; VPC/Private Subnet/RDS/ElastiCache 콘솔 증빙은 저장소 밖 |
| 민감 정보 관리 | 부분 충족 | GitHub Secrets + OIDC 사용 | [`.github/workflows/ci-cd.yml`](../.github/workflows/ci-cd.yml) ; 과제 권장인 AWS Parameter Store 연동은 현재 없음 |
| GitHub Actions CI 파이프라인 | 부분 충족 | 워크플로우, ECR push 단계 존재 | [`.github/workflows/ci-cd.yml`](../.github/workflows/ci-cd.yml) ; 현재 build 단계가 `-x test`라 테스트 강제 실행 항목은 추가 보완 필요 |
| GitHub Actions CD 파이프라인 | 부분 충족 | 자동 배포 워크플로우 존재 | [`.github/workflows/ci-cd.yml`](../.github/workflows/ci-cd.yml) ; SSM 방식이 아니라 Launch Template/ASG refresh 기반이고, 헬스체크 단계는 없다 |

## 3. 기능 별 발표 자료 연결

| 발표 주제 | 연결 문서 |
| --- | --- |
| 캐싱(Caching) | [`docs/artist-search-caching.md`](artist-search-caching.md), [`docs/member-artistcontent-design-rationale.md`](member-artistcontent-design-rationale.md) |
| 동시성 제어(Concurrency Control) | [`docs/artistcontent-concurrency-comparison.md`](artistcontent-concurrency-comparison.md), [`docs/concurrency-comparison.md`](concurrency-comparison.md) |
| 최적화(Indexing) | [`docs/index-analysis.md`](index-analysis.md), [`docs/member-artistcontent-index-analysis.md`](member-artistcontent-index-analysis.md) |
| 실시간 채팅 | [`docs/api-overview.md`](api-overview.md), [`WebSocketConfig.java`](../src/main/java/com/example/infinite/global/common/config/WebSocketConfig.java) |
| 배포와 CI/CD | [`README.md`](../README.md), [`.github/workflows/ci-cd.yml`](../.github/workflows/ci-cd.yml) |

## 4. 제출 전에 보완하면 좋은 항목

1. `CI`에서 실제 테스트가 돌도록 `-x test` 제거 여부 다시 확인하기
2. 배포 보안 항목에서 Parameter Store / 헬스체크 / SSM을 어디까지 적용했는지 최종 제출 전에 다시 정리하기
