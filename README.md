# INFINITE — Weverse Clone Platform

> 아티스트와 팬을 잇는 커뮤니티·실시간 소통 플랫폼  
> Spring Boot 4 · Java 21 · MySQL · Redis · WebSocket/STOMP

> 과제 체크리스트 바로가기: [`docs/assignment-checklist.md`](docs/assignment-checklist.md)

## 프로젝트 소개

INFINITE는 Weverse를 모티브로 한 팬 커뮤니티 플랫폼입니다. 아티스트는 게시글, DM, 라이브, 멤버십 콘텐츠로 팬과 소통하고, 팬은 커뮤니티 활동, 구독, 래플, 결제를 통해 플랫폼 안에서 아티스트와 연결됩니다.

이 프로젝트는 단순 기능 구현보다, 조회 성능과 데이터 일관성을 어떻게 나눠 설계할 것인가에 초점을 맞췄습니다. 검색 캐시, 게시글 `base/hot` 캐시, Redis Stream 기반 비동기 write-path, cursor 페이지네이션, 인덱스 검증을 통해 서비스형 백엔드의 읽기/쓰기 전략을 구현했습니다.

## 로컬 실행 방법

### 사전 조건

- Java 21
- Docker Desktop
- YouTube import 실연동 확인 시 `YOUTUBE_DATA_API_KEY` 환경변수

### 1. 로컬 인프라 기동

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
![WebSocket](https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socketdotio&logoColor=white)
![STOMP](https://img.shields.io/badge/STOMP-010101?style=for-the-badge)

### Infra / Frontend
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonwebservices&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![React](https://img.shields.io/badge/React_(CDN)-61DAFB?style=for-the-badge&logo=react&logoColor=black)

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

## 도메인 하이라이트

프로젝트 전체는 검색, 커뮤니티 콘텐츠, 개인화 홈, 결제/구독, 실시간 기능을 각기 다른 조회 전략과 일관성 모델로 나눠 구성했습니다.

### 검색 / 아티스트

- 아티스트 검색은 `v1 원본`, `v2 Caffeine`, `v3 Redis`로 분리해 캐시 전략 비교와 실사용 기본 경로를 함께 가져갔습니다.
- 인기 검색어는 `Redis ZSet`, 아티스트 상세는 `Redis cache-aside`로 분리해 검색과 상세 조회의 성격 차이를 반영했습니다.

### 아티스트 콘텐츠

- `FanPost`, `ArtistPost`, `FanLetter`를 중심으로 팬 커뮤니티, 아티스트 공지형 콘텐츠, 구독 전용 감정 표현형 콘텐츠를 분리했습니다.
- 게시글 조회는 `base cache + hot cache`, 댓글은 짧은 TTL 조건부 캐시를 적용해 읽기 패턴에 맞춰 최적화했습니다.
- `ArtistPost`는 가장 고트래픽 가능성이 큰 영역으로 보고 비동기 Redis Stream write-path를 사용합니다.

### 홈 / Follow / YouTube

- 메인 홈과 아티스트 홈은 하나의 거대한 피드가 아니라 여러 섹션을 조립하는 오케스트레이션 API로 설계했습니다.
- Follow는 일반 SNS 확장형이 아니라 `Member -> ArtistMember` 최소 모델로 남겨 개인화 홈에 집중했습니다.
- YouTube 탭은 파일 업로드가 아니라 외부 링크 아카이브로 분리해 카드형 미디어 기록에 초점을 맞췄습니다.

### 결제 / 구독 / 래플 / 실시간

- 결제는 젤리 원장과 비관적 락 기반 차감으로 정합성을 우선했습니다.
- 래플은 `Reservoir Sampling + Redis Lua Script`로 공정성과 원자성을 맞췄습니다.
- DM과 라이브 채팅은 `WebSocket/STOMP` 기반 실시간 흐름으로 구성했습니다.

## 담당별 구현 하이라이트

프로젝트는 도메인 단위로 역할을 분리하되, 공통 인프라와 API 계약을 공유하면서 각 담당 영역의 설계 포인트를 구체화했습니다.

### 임호진 · 인프라 / 인증·인가

- Docker 기반 로컬 실행 환경과 설정 프로파일을 정리해 팀원 개발 환경을 통일했습니다.
- Spring Security, JWT, role/status 기반 접근 제어를 구성해 `Member`, `Artist`, `Admin` 권한 경계를 정리했습니다.
- GitHub Actions와 배포 기반을 마련해 백엔드 변경을 지속적으로 검증할 수 있는 흐름을 만들었습니다.

### 배강혁 · 래플 / DM / 라이브 / 프론트 통합

- 래플은 `Redis Lua Script`와 `Reservoir Sampling`으로 동시 응모와 당첨자 선정을 원자적으로 처리했습니다.
- DM과 라이브는 `WebSocket/STOMP` 기반 실시간 메시징, 채팅 조회, 운영용 관리 API까지 함께 구성했습니다.
- 정적 React 목업과 실제 백엔드 API를 연결해 주요 도메인의 프론트 실연동 흐름을 정리했습니다.

### 황도윤 · 아티스트 콘텐츠 / 커뮤니티 / 검색 / 캐싱

- 아티스트 검색은 `v1 원본`, `v2 Caffeine`, `v3 Redis`로 경로를 분리하고, 인기 검색어는 `Redis ZSet`, 상세 조회는 `Redis cache-aside`로 최적화했습니다.
- `FanPost`, `ArtistPost`, `FanLetter`에는 `base cache + hot cache`를 적용하고, 댓글은 short cache와 depth 2 + mention 정책으로 분리했습니다.
- `ArtistPost`는 `no lock / Lettuce v1 / Redisson v2 / Stream v3` 비교를 거쳐 Redis Stream 기반 비동기 write-path로 고트래픽 구간의 최종 수렴을 검증했습니다.
- 메인 홈과 아티스트 홈을 섹션 오케스트레이션 API로 설계하고, Follow는 `Member -> ArtistMember` 최소 모델, YouTube는 snapshot 기반 외부 링크 아카이브로 정리했습니다.

`member + artistcontent` 상세 문서:

- README/포트폴리오용 축약본: [`docs/member-artistcontent-readme-summary.md`](docs/member-artistcontent-readme-summary.md)
- 설계 근거 문서: [`docs/member-artistcontent-design-rationale.md`](docs/member-artistcontent-design-rationale.md)
- 인덱스 분석 문서: [`docs/member-artistcontent-index-analysis.md`](docs/member-artistcontent-index-analysis.md)

### 정민교 · 결제 / 구독 / 멤버십

- PortOne 결제 준비/웹훅 반영, 빌링키 등록, 자동결제, 환불 API로 결제 흐름을 단계별로 분리했습니다.
- 젤리 원장과 비관적 락 기반 차감으로 결제·차감 정합성을 우선했습니다.
- 멤버십과 DM 구독의 가입, 상태 조회, 이력 API를 제공해 후속 콘텐츠 권한 판단 기준을 만들었습니다.


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
