# INFINITE — Weverse Clone Platform

> 아티스트와 팬을 잇는 커뮤니티·실시간 소통 플랫폼  
> Spring Boot 4 · Java 21 · MySQL · Redis · WebSocket/STOMP

> 과제 체크리스트 바로가기: [`docs/assignment-checklist.md`](docs/assignment-checklist.md)

## 프로젝트 소개

INFINITE는 Weverse를 모티브로 한 팬 커뮤니티 플랫폼입니다. 아티스트는 게시글, DM, 라이브, 멤버십 콘텐츠로 팬과 소통하고, 팬은 커뮤니티 활동, 구독, 래플, 결제를 통해 플랫폼 안에서 아티스트와 연결됩니다.

이 프로젝트는 단순 기능 구현보다, 조회 성능과 데이터 일관성을 어떻게 나눠 설계할 것인가에 초점을 맞췄습니다. 검색 캐시, 게시글 `base/hot` 캐시, Redis Stream 기반 비동기 write-path, cursor 페이지네이션, 인덱스 검증을 통해 서비스형 백엔드의 읽기/쓰기 전략을 구현했습니다.

### 학습 목표

Redis 기반 동시성 제어와 캐싱, WebSocket/STOMP 실시간 통신, CI/CD 파이프라인 구축 등 고급 백엔드 기술 역량을 실전 프로젝트를 통해 확보하는 것이 목적입니다.

## 로컬 실행 방법

### 사전 조건

- Java 21
- Docker Desktop
- YouTube import 실연동 확인 시 `YOUTUBE_DATA_API_KEY` 환경변수

### 1. 로컬 인프라 기동

저장소에 포함된 `docker-compose.yml` 기준으로 MySQL/Redis를 함께 띄웁니다.

```bash
docker compose up -d
```

기본 포트:

- MySQL: `33306`
- Redis: `6380`

### 2. 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

기본 로컬 설정:

- DB: `springchatting`
- 테스트 DB: `springchatting_test`
- Redis: `localhost:6380`
- `media.storage.enabled=false`

참고:

- 기본 로컬 프로필은 스토리지 업로드를 끈 상태라, 파일 업로드 실연동은 `media.storage.*` 설정이 별도로 필요합니다.

### 3. 수동 실행 방식

기존 수동 실행 방식도 유지합니다. 다만 현재 팀 기본 로컬 포트는 `33306 / 6380` 입니다.

```bash
# 1. DB 생성
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS infinite;"

# 2. Redis 기동
docker run -d --name redis -p 6379:6379 redis:7-alpine

# 3. 애플리케이션 실행
./gradlew bootRun
```

`application.yml` 또는 로컬 프로필의 datasource 설정을 개인 환경에 맞춰 조정하세요.

### 4. 동시성 테스트 실행

```bash
# 테스트 DB 생성
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS infinite_test;"

# 테스트 실행 (-i 옵션으로 결과 출력 확인)
./gradlew test --tests "*.JellyConcurrencyTest" -i
```

## 팀 구성 / 담당 범위

도메인 단위로 역할을 나눠 백엔드 기능 구현, 실시간 기능, 프론트 실연동을 병행했습니다.

| 역할 | 이름 | 담당 도메인 |
| --- | --- | --- |
| 팀장 | 임호진 | 인프라, CI/CD, 인증/인가 |
| 부팀장 | 배강혁 | 래플, DM, 라이브 스트리밍, 프론트 통합 |
| 팀원 | 황도윤 | 아티스트 콘텐츠, 커뮤니티, 검색, 캐싱 |
| 팀원 | 정민교 | 결제, 구독, 멤버십 |

## 기술 스택

이 프로젝트는 `Spring Boot + MySQL + Redis`를 중심으로, 실시간 통신과 캐싱, 비동기 write-path를 함께 다루는 구조로 구성했습니다.

### Backend
![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL_5.1-4479A1?style=for-the-badge)

### Database / Cache / Realtime
![MySQL](https://img.shields.io/badge/MySQL_8.4-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Redisson](https://img.shields.io/badge/Redisson_4.0-DC382D?style=for-the-badge)
![WebSocket](https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socketdotio&logoColor=white)
![STOMP](https://img.shields.io/badge/STOMP-010101?style=for-the-badge)
![Redis_Pub/Sub](https://img.shields.io/badge/Redis_Pub/Sub-DC382D?style=for-the-badge&logo=redis&logoColor=white)

### Infra / Frontend / Monitoring
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonwebservices&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![React](https://img.shields.io/badge/React_(CDN)-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)
![PortOne](https://img.shields.io/badge/PortOne-5B2C6F?style=for-the-badge)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white)

주요 활용 방식:

- `Spring Boot`, `JPA`, `QueryDSL`: REST API, 도메인 로직, 동적 조회 쿼리
- `MySQL`: 트랜잭션 원본 데이터 저장소
- `Redis`: 조회 캐시, 인기 검색어 집계, ArtistPost 비동기 write-path
- `WebSocket/STOMP`: DM, 라이브 채팅, 실시간 이벤트 전달
- `Docker`: 로컬 MySQL/Redis 실행 환경 통일
- `React(CDN)`: 정적 프론트 목업과 백엔드 실연동
- `AWS`, `GitHub Actions`: 배포 및 자동화 확장 기반

## 시스템 아키텍처

```text
Client (React SPA / static)
    │
    ├── REST API ─────── Spring Boot ─────── MySQL
    │                         │
    ├── WebSocket/STOMP ──────┤
    │                         │
    └── PortOne Webhook ──────┤
                              │
                           Redis
                  ┌───────────┼───────────┐
               Cache        Streams     Sorted Set
            (조회 최적화)  (비동기 쓰기) (인기 검색어)
```

핵심 구조는 `MySQL`을 원본 저장소로 두고, `Redis`를 읽기 최적화와 비동기 버퍼 계층으로 함께 사용하는 방식입니다. 일반 조회는 `REST API -> Cache -> DB fallback` 흐름을 따르고, 고트래픽 쓰기 구간은 `Redis Stream`을 거쳐 최종 read model이 수렴하도록 구성했습니다.

주요 흐름:

- 조회 경로: `Client -> Spring Boot -> Redis Cache -> MySQL`
- 고트래픽 쓰기 경로: `Client -> Spring Boot -> Redis Stream -> DB flush/reconcile`
- 실시간 경로: `Client <-> WebSocket/STOMP <-> Spring Boot`, 필요 시 Redis Pub/Sub 연계
- 결제 경로: `Client -> Spring Boot -> PortOne`, 이후 webhook으로 결제 결과 반영

## API 개요

README에는 API 전체 명세 대신 도메인별 범위와 참고 문서만 요약합니다.

| 도메인 | 주요 기능 | 상세 |
| --- | --- | --- |
| Member / Artist | 아티스트 검색, 인기 검색어, 상세 조회, 아티스트/멤버 관리 | [`docs/api-overview.md`](docs/api-overview.md) |
| Home / Follow | 메인 홈 대시보드, 아티스트 홈 대시보드, ArtistMember 팔로우 | [`docs/api-overview.md`](docs/api-overview.md) |
| Post / Comment | FanPost, ArtistPost, FanLetter CRUD, 좋아요, 댓글/대댓글 | [`docs/api-overview.md`](docs/api-overview.md) |
| Media / VOD | YouTube 카드 import/list, 종료 라이브 VOD 조회 | [`docs/api-overview.md`](docs/api-overview.md) |
| Payment / Subscription | 결제, 젤리, 멤버십/구독 | [`docs/api-overview.md`](docs/api-overview.md) |
| Raffle / DM / Live | 래플 응모, DM, 라이브 채팅 | [`docs/api-overview.md`](docs/api-overview.md) |

공통 응답과 페이지네이션 규칙은 `ApiResponse`, `CursorSliceResponse`, `ScoreCursorSliceResponse`, `OffsetSliceResponse` 중심으로 정리했습니다.

- API 빠른 참조: [`docs/api-overview.md`](docs/api-overview.md)
- 프론트 연동 참고: [`FRONTEND_TEAM_VIBE_HANDOFF.md`](FRONTEND_TEAM_VIBE_HANDOFF.md)

## 도메인별 기능 / 담당 영역

### 검색 / 아티스트 / 홈 / Follow / YouTube / 아티스트 콘텐츠 (황도윤)

`member + artistcontent` 축은 검색, 아티스트 페이지 콘텐츠, 개인화 홈을 함께 묶어 읽기 최적화와 데이터 일관성 전략을 설계했습니다.

**검색 / 상세 조회**: 아티스트 검색은 `v1 원본`, `v2 Caffeine`, `v3 Redis`로 분리해 캐시 전략 비교와 실사용 기본 경로를 함께 가져갔습니다. 인기 검색어는 `Redis ZSet`, 아티스트 상세는 `Redis cache-aside`로 분리해 검색과 상세 조회의 성격 차이를 반영했습니다.

**콘텐츠 / 캐싱**: `FanPost`, `ArtistPost`, `FanLetter`를 중심으로 팬 커뮤니티, 아티스트 공지형 콘텐츠, 감정 표현형 콘텐츠를 분리했습니다. 게시글 조회에는 `base cache + hot cache`, 댓글에는 short cache와 depth 2 + mention 정책을 적용했습니다.

**동시성 / write-path**: `ArtistPost`는 가장 고트래픽 가능성이 큰 영역으로 보고 `no lock / Lettuce v1 / Redisson v2 / Stream v3` 비교를 거쳐 Redis Stream 기반 비동기 write-path를 적용했습니다.

**홈 / Follow / YouTube**: 메인 홈과 아티스트 홈은 여러 섹션을 조립하는 오케스트레이션 API로 설계했습니다. Follow는 `Member -> ArtistMember` 최소 모델로 남겨 개인화 홈에 집중했고, YouTube 탭은 snapshot 기반 외부 링크 아카이브로 정리했습니다.

`member + artistcontent` 상세 문서:

- README/포트폴리오용 축약본: [`docs/member-artistcontent-readme-summary.md`](docs/member-artistcontent-readme-summary.md)
- 설계 근거 문서: [`docs/member-artistcontent-design-rationale.md`](docs/member-artistcontent-design-rationale.md)
- 인덱스 분석 문서: [`docs/member-artistcontent-index-analysis.md`](docs/member-artistcontent-index-analysis.md)

### 래플 시스템 (배강혁)

시간 슬롯 기반 Reservoir Sampling으로 수학적 공정성(1/n 확률)을 보장하는 추첨 시스템입니다.

**핵심 아키텍처**: 모집 시간 `t`를 당첨 수 `p`개 슬롯으로 균등 분할하고, 슬롯당 Redis 키 2개(INCR 카운터 + 현재 후보 `userId`)만으로 `O(1)` 메모리에서 운영합니다. 응모와 슬롯 종료를 각각 Lua Script(`entry.lua`, `close_slot.lua`)로 원자화하여 네트워크 역전이나 동시 요청에도 정합성을 보장합니다.

**감사 로그**: Redis Streams(MAXLEN 100,000) → Consumer Group → `JdbcTemplate.batchUpdate()`로 DB 배치 저장. 래플 완료 시 자동 소비 및 스트림 삭제.

**당첨 보상**: 슬롯 종료 시 자동 지급(DM 구독 +30일), 당첨 알림은 STOMP Push, 비당첨 알림에는 Jitter(0~5초)를 적용하여 서버 부하 스파이크를 방지합니다.

### DM 실시간 채팅 (배강혁)

1:N 비대칭 구조의 실시간 메시징입니다. 아티스트가 1회 발송하면 전체 구독 유저의 DM 방에 fan-out됩니다.

**주요 설계**: STOMP `/pub/dm/broadcast/{artistId}`로 일괄 발송, 개별 메시지는 `/pub/dm/{roomId}`. 답장 제한(아티스트 답장 전 유저 최대 3건), 구독 상태 매 메시지 검증, 커서 기반 페이징(`id DESC`).

**도메인 경계**: 타 도메인(결제)에서 환불 검증 시 `DmService.hasUserSentMessageAfter()` 공개 인터페이스를 통해 접근합니다. 레포지토리 직접 노출은 하지 않습니다.

### 라이브 스트리밍 + 채팅 (배강혁)

STOMP 배치 브로드캐스트 기반의 실시간 채팅 시스템입니다.

**배치 구조**: `liveId`별 `ConcurrentLinkedQueue` 인메모리 버퍼 + `@Scheduled(fixedRate=300)` flush. 다중 서버 대응을 위해 Redis Pub/Sub `live:chat:broadcast` 채널을 경유합니다.

**쓰로틀링**: 4중 검증(LIVE 상태 → 뮤트 확인 → 슬라이딩 윈도우 2건/초/유저 → 200자 제한). 위반 메시지는 조용히 무시합니다.

**LIVE 상태 캐싱**: Redis `live:{liveId}:status`에 EXISTS 검증으로 DB 쿼리를 제거합니다. 쓰기 2회(start/end) vs 읽기 N회의 극단적 비대칭이 근거입니다.

### 결제·구독·멤버십 (정민교)

젤리 기반 플랫폼 내 재화 시스템과 PortOne 연동 결제입니다.

**젤리**: 1 Jelly = 300원. 원장(Ledger) 기반 이력 관리, 비관적 락(`SELECT ... FOR UPDATE`)으로 동시 차감 시 Double Spending 방지.

**구독**: Fan Membership(9 Jelly), DM Subscription(15 Jelly). 만료 스케줄러, 래플 당첨 시 자동 연장(+30일).

**자동 충전**: 빌링키 등록 → 임계값 이하 시 자동 결제.

### 인증/인가·인프라 (임호진)

JWT 기반 인증, Spring Security, STOMP ChannelInterceptor JWT 검증, Docker 컨테이너화, AWS 배포, GitHub Actions CI/CD.

## 핵심 설계 질문

아래 질문에 대한 답변 본문은 README에 길게 쓰지 않고 별도 문서로 분리했습니다.

- 왜 게시글은 `base cache + hot cache`인데 댓글은 short cache로 남겼는가?
- 왜 모든 쓰기를 복잡하게 만들지 않고 `ArtistPost`에만 비동기 Redis Stream 경로를 적용했는가?
- 왜 최신순 피드에는 `offset`보다 `cursor`를 우선했고, HOT 목록은 도메인마다 다른 페이지네이션을 남겼는가?
- 왜 Follow를 일반 SNS 모델이 아니라 `Member -> ArtistMember` 최소 모델로 정리했는가?
- 왜 홈 대시보드는 캐시보다 섹션 오케스트레이션을 먼저 고정했는가?

읽는 순서:

- 빠른 요약: [`docs/member-artistcontent-readme-summary.md`](docs/member-artistcontent-readme-summary.md)
- 상세 설계 근거: [`docs/member-artistcontent-design-rationale.md`](docs/member-artistcontent-design-rationale.md)

## 성능 / 검증 결과 요약

아래 수치는 로컬 `Docker + 단일 애플리케이션 인스턴스` 기준 대표 결과만 요약한 것입니다. 실험 조건, 측정 방식, 쿼리/캐시 해석은 각 문서에 따로 정리했습니다.

### 캐싱

| 대상 | 결과 | 상세 |
| --- | --- | --- |
| 검색 `v1 -> v2 warm` | `99.5%` 개선 | [`docs/artist-search-caching.md`](docs/artist-search-caching.md) |
| 검색 `v1 -> v3 warm` | `86.5%` 개선 | [`docs/artist-search-caching.md`](docs/artist-search-caching.md) |
| 아티스트 상세 `v1 -> Redis warm` | `75.5%` 개선 | [`docs/artist-search-caching.md`](docs/artist-search-caching.md) |
| ArtistPost base list `uncached -> warm` | `85.9%` 개선 | [`docs/artistcontent-post-caching-benchmark.md`](docs/artistcontent-post-caching-benchmark.md) |
| ArtistPost base detail `uncached -> warm` | `87.7%` 개선 | [`docs/artistcontent-post-caching-benchmark.md`](docs/artistcontent-post-caching-benchmark.md) |

### 동시성 / 수렴

| 대상 | 결과 | 상세 |
| --- | --- | --- |
| 좋아요 `20,000`, `30,000` burst | 로컬 정합성 수렴 확인 | [`docs/artistcontent-concurrency-comparison.md`](docs/artistcontent-concurrency-comparison.md) |
| 댓글 `12,000` burst | 로컬 정합성 수렴 확인 | [`docs/artistcontent-concurrency-comparison.md`](docs/artistcontent-concurrency-comparison.md) |
| mixed `10,000` 작업 | 로컬 정합성 수렴 확인 | [`docs/artistcontent-concurrency-comparison.md`](docs/artistcontent-concurrency-comparison.md) |
| 결제 락 3종 비교 | no lock / pessimistic / optimistic 비교 | [`docs/concurrency-comparison.md`](docs/concurrency-comparison.md) |

### 인덱스

| 대상 | 결과 | 상세 |
| --- | --- | --- |
| DM / Raffle / Live | Before / After EXPLAIN 정리 | [`docs/index-analysis.md`](docs/index-analysis.md) |
| Member / ArtistContent | Before / After EXPLAIN + 응답시간 비교 | [`docs/member-artistcontent-index-analysis.md`](docs/member-artistcontent-index-analysis.md) |

## 추가 검증 기록

### 동시성 제어 3종 비교

젤리 동시 차감 시나리오에서 3가지 락 전략을 100개 스레드 × 100 젤리 차감으로 비교했습니다.

| 전략 | 성공 | 실패 | 최종 잔액 | 정합성 | 비고 |
| --- | --- | --- | --- | --- | --- |
| 락 없음 (read-modify-write) | 100 | 0 | 9,900 | ❌ | Lost Update 약 99건 |
| 비관적 락 (`FOR UPDATE`) | 100 | 0 | 0 | ✅ | 순차 처리, 전부 성공 |
| 낙관적 락 (`@Version`) | 1 | 99 | 9,900 | ✅ | 버전 충돌로 1건만 성공 |

> 자세한 분석은 [`docs/concurrency-comparison.md`](docs/concurrency-comparison.md)를 참고하세요.

**프로젝트의 선택 — 비관적 락**: 결제 도메인에서 차감 실패는 UX 저하이고, 자동충전 등 후속 로직의 부수효과 관리가 복잡하므로 "대기하더라도 전부 성공"하는 비관적 락이 적합합니다.

### 인덱스 설계 및 EXPLAIN 검증

5만건+ 더미 데이터에서 Before/After EXPLAIN을 비교했습니다.

| 쿼리 | Before | After | 개선 |
| --- | --- | --- | --- |
| `DmRoom.findByArtistId` | `ALL, rows=100` | `ref, rows=20` | `5x` |
| `RaffleEntry.findByUserIdOrderByEnteredAtDesc` | `ALL, rows=49,504 + filesort` | `ref, rows=50 + Backward scan` | `990x`, filesort 제거 |

> 자세한 분석은 [`docs/index-analysis.md`](docs/index-analysis.md)를 참고하세요.

### 프론트엔드 통합

React(CDN) + Tailwind CSS로 서버 빌드 의존 없이 프론트엔드를 구성했습니다. Spring Boot의 `static/` 디렉토리에서 직접 서빙합니다.

**규모**: 11개 JSX + `index.html` = 6,122줄, REST API 26건, STOMP 7개 함수

**주요 화면**: 메인 대시보드, 아티스트 홈, Fan Post/Artist Post/Fan Letter CRUD, HOT 탭(ScoreCursor + Offset), 라이브 채팅, DM, 래플 응모, 구독 관리, 프로필

## 상세 문서

README에는 요약만 남기고, API와 캐시/동시성/인덱스 분석은 아래 문서로 분리했습니다.

| 문서 | 설명 |
| --- | --- |
| [`docs/api-overview.md`](docs/api-overview.md) | 도메인별 API 빠른 참조 |
| [`docs/artist-search-caching.md`](docs/artist-search-caching.md) | 검색 `v1/v2/v3`, 상세 Redis 캐시, 인기 검색어 분석 |
| [`docs/artistcontent-post-caching-benchmark.md`](docs/artistcontent-post-caching-benchmark.md) | ArtistPost base/hot 캐시 벤치마크 |
| [`docs/artistcontent-concurrency-comparison.md`](docs/artistcontent-concurrency-comparison.md) | ArtistPost `v1/v2/v3` 동시성·스트림 비교 |
| [`docs/concurrency-comparison.md`](docs/concurrency-comparison.md) | 결제 도메인 락 3종 비교 |
| [`docs/member-artistcontent-readme-summary.md`](docs/member-artistcontent-readme-summary.md) | `member + artistcontent` README/포트폴리오용 축약본 |
| [`docs/member-artistcontent-design-rationale.md`](docs/member-artistcontent-design-rationale.md) | `member + artistcontent` 설계 이유 정리 |
| [`docs/member-artistcontent-index-analysis.md`](docs/member-artistcontent-index-analysis.md) | `member + artistcontent` 인덱스 분석 |
| [`docs/index-analysis.md`](docs/index-analysis.md) | 전체 프로젝트 인덱스 분석 |

## 프로젝트 구조

프로젝트는 도메인별 패키지와 공통 인프라 계층을 분리해, 기능 추가와 실험 코드가 서로 섞이지 않도록 구성했습니다.

```text
src/main/java/com/example/infinite/
├── domain/
│   ├── artistcontent/
│   │   └── media/
│   ├── dm/
│   ├── member/
│   ├── payment/
│   ├── raffle/
│   ├── realtimelive/
│   └── subscriptionmembership/
├── global/
│   ├── auth/
│   ├── common/
│   ├── error/
│   └── lock/
└── resources/
    └── redis/
```

기존 상세 구조 메모:

```text
src/main/java/com/example/infinite/
├── domain/
│   ├── artistcontent/    # 게시글·댓글·리액션·해시태그·미디어
│   ├── dm/               # DM 룸·메시지
│   ├── member/           # 회원·아티스트·팔로우
│   ├── payment/          # 젤리·결제·빌링
│   ├── raffle/           # 래플·슬롯·Reservoir Sampling
│   ├── realtimelive/     # 라이브 스트리밍·채팅
│   └── subscriptionmembership/  # 구독·멤버십
├── global/
│   ├── auth/             # JWT·Security·STOMP 인증
│   ├── common/           # 설정·DTO·캐시·QueryDSL 유틸
│   ├── error/            # 글로벌 예외 처리
│   ├── lock/             # 분산 락
│   └── s3/               # 파일 업로드
└── resources/
    └── redis/            # Lua Scripts
```

구조 요약:

- `domain`: 비즈니스 기능별 도메인 계층
- `domain/artistcontent/media`: S3 호환 오브젝트 스토리지 어댑터와 미디어 업로드 처리
- `global`: 인증, 공통 설정, 예외 처리, 락 같은 횡단 관심사
- `resources/redis`: Lua Script, Redis 관련 보조 리소스

## 트레이드오프 / 한계

- 검색과 일부 읽기 경로는 강한 실시간 일관성보다 `TTL 기반 eventual consistency`를 우선했습니다.
- 모든 도메인에 동일한 복잡도를 적용하지 않고, 고트래픽 가능성이 큰 `ArtistPost`에만 무거운 비동기 write-path를 집중했습니다.
- 홈 대시보드는 캐시 최적화보다 응답 shape와 섹션 정책 고정을 먼저 선택했습니다.
- 성능 검증은 로컬 `Docker + 단일 인스턴스` 기준 결과이므로, 운영 환경 수용량으로 그대로 해석하면 안 됩니다.
- README는 포트폴리오용 요약 문서로 유지하고, 세부 설계 근거와 검증 과정은 `docs`로 분리했습니다.

### 세부 메모

#### 1. 캐싱 미적용 — 래플·DM·라이브 도메인

강혁 담당 3개 도메인에는 Redis 캐시(`@Cacheable`)를 적용하지 않았습니다.

**래플**: 응모 데이터는 이미 Redis(INCR + candidate)에서 처리되고, 조회(래플 목록/상세)는 아티스트당 수십 건 수준이라 DB 직접 조회로 충분합니다. 캐시를 넣으면 래플 상태 전이(PENDING → ACTIVE → COMPLETED) 시 무효화 로직만 복잡해집니다.

**DM**: 메시지는 실시간 STOMP로 전달되고, REST 조회는 커서 페이징으로 이력을 불러오는 용도입니다. 매번 다른 커서 값이 오므로 캐시 히트율이 극히 낮습니다.

**라이브 채팅**: 채팅 메시지는 인메모리 버퍼(`ConcurrentLinkedQueue`) → 배치 flush → DB 저장 구조로, 이미 읽기 경로에 DB를 타지 않습니다. LIVE 상태 검증은 Redis EXISTS로 대체하여 캐시의 필요가 없습니다.

#### 2. 감사 로그 ACK-batchInsert 순서 트레이드오프

현재 `RaffleAuditConsumer`에서 `batchInsert()` 내부의 예외를 catch로 삼키고 있어, DB 저장에 실패해도 Redis 측 ACK가 진행됩니다. 이상적으로는 batchInsert 성공 후에만 ACK해야 하지만, Redis Streams Consumer Group의 재시도 메커니즘(PEL 관리)까지 구현하면 범위가 커지므로, 교육 프로젝트에서는 "로그 레벨 ERROR로 실패를 기록하고, 운영 환경에서는 DLQ 또는 재시도 로직이 필요하다"는 점을 인지한 상태로 남겨두었습니다.

#### 3. `ddl-auto: validate` 환경에서의 `@Index` 반영

엔티티에 `@Index`를 선언하면 `ddl-auto: create/update` 환경에서는 자동 생성되지만, 프로덕션의 `validate` 모드에서는 스키마 변경이 일어나지 않습니다. 운영 환경에서는 Flyway나 Liquibase 같은 마이그레이션 도구가 필요하며, 이 사실을 인지하고 있습니다. 현재 프로젝트에는 마이그레이션 도구를 도입하지 않았는데, 제출 일정과 프로젝트 규모를 고려한 판단입니다.

#### 4. Testcontainers 미사용

초기에 Testcontainers로 MySQL/Redis 컨테이너를 자동 기동하는 통합 테스트를 설계했으나, Docker Desktop 29.x와의 호환 이슈(Ryuk 컨테이너 실행 실패)로 로컬 컨테이너 방식으로 전환했습니다. `application-test.yml`에 사전 조건을 명시하고, README에 실행 환경 세팅 가이드를 포함하여 다른 팀원의 로컬 환경에서도 동작을 보장합니다.

#### 5. Reservoir Sampling `k>1` (`entry_multi.lua`) 미구현

현재 `entry.lua`는 슬롯당 1명(`k=1`) 전용입니다. carry-over(이월)로 이전 슬롯에서 당첨자가 없을 때 다음 슬롯의 `k`가 2 이상이 되는 시나리오에서는 `entry_multi.lua`(가중 Reservoir Sampling)가 필요하지만, 현재 구현에서는 carry-over 시 `target_winner_count`만 증가시키고 실제 다중 후보 관리는 하지 않습니다. `k=1`로도 MVP 시나리오는 충분히 커버되며, `k>1` 확장은 향후 과제로 남겨두었습니다.

#### 6. k6 부하 테스트 미실시

CI/CD 파이프라인이 프로젝트 후반까지 불안정했기 때문에, 배포 환경 기반 부하 테스트를 실시하지 못했습니다. 대신 로컬 환경에서 JUnit + `CyclicBarrier` 기반 동시성 테스트와 EXPLAIN 기반 쿼리 성능 검증으로 대체했습니다.
