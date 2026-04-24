# INFINITE — Weverse Clone Platform

> 아티스트와 팬을 잇는 실시간 소통 플랫폼  
> Spring Boot 4.0.0 · Java 21 · MySQL · Redis · WebSocket/STOMP

---

## 프로젝트 소개

INFINITE는 Weverse를 모티브로 한 팬 커뮤니티 플랫폼입니다. 아티스트가 게시글·DM·라이브 스트리밍으로 팬과 소통하고, 팬은 커뮤니티 활동·래플 참여·구독을 통해 아티스트와 교감합니다. 플랫폼 내 재화 "젤리(Jelly)"를 통한 결제 시스템과, Reservoir Sampling 기반 공정 추첨 시스템이 핵심 기술적 차별점입니다.

### 학습 목표

Redis 기반 동시성 제어와 캐싱, WebSocket/STOMP 실시간 통신, CI/CD 파이프라인 구축 등 고급 백엔드 기술 역량을 실전 프로젝트를 통해 확보하는 것이 목적입니다.

---

## 팀 구성

| 역할 | 이름 | 담당 도메인 |
|------|------|------------|
| 팀장 | 임호진 | 인프라·CI/CD·인증/인가 |
| 부팀장 (백엔드 서브리더) | 배강혁 | 래플·DM·라이브 스트리밍·프론트엔드 통합 |
| 팀원 | 황도윤 | 아티스트 콘텐츠·커뮤니티·검색·캐싱 |
| 팀원 | 정민교 | 결제·구독·멤버십 |

---

## 기술 스택

#### Backend
![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL_5.1-4479A1?style=for-the-badge)

#### Database & Cache
![MySQL](https://img.shields.io/badge/MySQL_8.4-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Redisson](https://img.shields.io/badge/Redisson_4.0-DC382D?style=for-the-badge)

#### Realtime
![WebSocket](https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socketdotio&logoColor=white)
![STOMP](https://img.shields.io/badge/STOMP-010101?style=for-the-badge)
![Redis Pub/Sub](https://img.shields.io/badge/Redis_Pub/Sub-DC382D?style=for-the-badge&logo=redis&logoColor=white)

#### Infra & CI/CD
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonwebservices&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

#### Payment & Monitoring
![PortOne](https://img.shields.io/badge/PortOne-5B2C6F?style=for-the-badge)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white)

#### Frontend
![React](https://img.shields.io/badge/React_(CDN)-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)

---

## 시스템 아키텍처

```
Client (React SPA)
    │
    ├── REST API ──────── Spring Boot ──── MySQL
    │                         │
    ├── WebSocket/STOMP ──────┤
    │                         │
    └── PortOne Webhook ──────┤
                              │
                         Redis Cluster
                    ┌─────────┼──────────┐
                 Pub/Sub   Streams   Sorted Set
              (라이브채팅)  (감사로그)  (쓰로틀링)
```

---

## 도메인별 기능

### 래플 시스템 (배강혁)

시간 슬롯 기반 Reservoir Sampling으로 수학적 공정성(1/n 확률)을 보장하는 추첨 시스템입니다.

**핵심 아키텍처**: 모집 시간 t를 당첨 수 p개 슬롯으로 균등 분할하고, 슬롯당 Redis 키 2개(INCR 카운터 + 현재 후보 userId)만으로 O(1) 메모리에서 운영합니다. 응모와 슬롯 종료를 각각 Lua Script(`entry.lua`, `close_slot.lua`)로 원자화하여 네트워크 역전이나 동시 요청에도 정합성을 보장합니다.

**감사 로그**: Redis Streams(MAXLEN 100,000) → Consumer Group → `JdbcTemplate.batchUpdate()`로 DB 배치 저장. 래플 완료 시 자동 소비 및 스트림 삭제.

**당첨 보상**: 슬롯 종료 시 자동 지급(DM 구독 +30일), 당첨 알림은 STOMP Push, 비당첨 알림에는 Jitter(0~5초)를 적용하여 서버 부하 스파이크를 방지합니다.

### DM 실시간 채팅 (배강혁)

1:N 비대칭 구조의 실시간 메시징입니다. 아티스트가 1회 발송하면 전체 구독 유저의 DM 방에 fan-out됩니다.

**주요 설계**: STOMP `/pub/dm/broadcast/{artistId}`로 일괄 발송, 개별 메시지는 `/pub/dm/{roomId}`. 답장 제한(아티스트 답장 전 유저 최대 3건), 구독 상태 매 메시지 검증, 커서 기반 페이징(id DESC).

**도메인 경계**: 타 도메인(결제)에서 환불 검증 시 `DmService.hasUserSentMessageAfter()` 공개 인터페이스를 통해 접근합니다. 레포지토리 직접 노출은 하지 않습니다.

### 라이브 스트리밍 + 채팅 (배강혁)

STOMP 배치 브로드캐스트 기반의 실시간 채팅 시스템입니다.

**배치 구조**: liveId별 `ConcurrentLinkedQueue` 인메모리 버퍼 + `@Scheduled(fixedRate=300)` flush. 다중 서버 대응을 위해 Redis Pub/Sub `live:chat:broadcast` 채널을 경유합니다.

**쓰로틀링**: 4중 검증(LIVE 상태 → 뮤트 확인 → 슬라이딩 윈도우 2건/초/유저 → 200자 제한). 위반 메시지는 조용히 무시합니다.

**LIVE 상태 캐싱**: Redis `live:{liveId}:status`에 EXISTS 검증으로 DB 쿼리를 제거합니다. 쓰기 2회(start/end) vs 읽기 N회의 극단적 비대칭이 근거입니다.

### 아티스트 콘텐츠 (황도윤)

Fan Post, Artist Post, Fan Letter, 미디어(YouTube 연동), 댓글, 리액션, 해시태그, 팔로우를 포함하는 커뮤니티 콘텐츠 도메인입니다.

**캐싱**: Caffeine(로컬) → Redis(글로벌) 2단계 캐시 전략. base/hot 분리, 조건부 admission 정책 적용.

**동시성 테스트**: 10,000명 동시 좋아요(128스레드), 1,200명 동시 댓글(96스레드), 혼합 4,200건 동시 실행 — 모두 정합성 유지 성공.

**검색**: QueryDSL 동적 쿼리 + Redis Sorted Set 인기 검색어 랭킹.

### 결제·구독·멤버십 (정민교)

젤리 기반 플랫폼 내 재화 시스템과 PortOne 연동 결제입니다.

**젤리**: 1 Jelly = 300원. 원장(Ledger) 기반 이력 관리, 비관적 락(`SELECT ... FOR UPDATE`)으로 동시 차감 시 Double Spending 방지.

**구독**: Fan Membership(9 Jelly), DM Subscription(15 Jelly). 만료 스케줄러, 래플 당첨 시 자동 연장(+30일).

**자동 충전**: 빌링키 등록 → 임계값 이하 시 자동 결제.

### 인증/인가·인프라 (임호진)

JWT 기반 인증, Spring Security, STOMP ChannelInterceptor JWT 검증, Docker 컨테이너화, AWS 배포, GitHub Actions CI/CD.

---

## 동시성 제어 3종 비교

젤리 동시 차감 시나리오에서 3가지 락 전략을 100개 스레드 × 100 젤리 차감으로 비교했습니다.

| 전략 | 성공 | 실패 | 최종 잔액 | 정합성 | 비고 |
|------|------|------|-----------|--------|------|
| 락 없음 (read-modify-write) | 100 | 0 | 9,900 | ❌ | Lost Update 약 99건 |
| 비관적 락 (`FOR UPDATE`) | 100 | 0 | 0 | ✅ | 순차 처리, 전부 성공 |
| 낙관적 락 (`@Version`) | 1 | 99 | 9,900 | ✅ | 버전 충돌로 1건만 성공 |

> 자세한 분석은 [`docs/concurrency-comparison.md`](docs/concurrency-comparison.md)를 참고하세요.

**프로젝트의 선택 — 비관적 락**: 결제 도메인에서 차감 실패는 UX 저하이고, 자동충전 등 후속 로직의 부수효과 관리가 복잡하므로 "대기하더라도 전부 성공"하는 비관적 락이 적합합니다.

---

## 인덱스 설계 및 EXPLAIN 검증

5만건+ 더미 데이터에서 Before/After EXPLAIN을 비교했습니다.

| 쿼리 | Before | After | 개선 |
|------|--------|-------|------|
| DmRoom.findByArtistId | ALL, rows=100 | ref, rows=20 | 5× |
| RaffleEntry.findByUserIdOrderByEnteredAtDesc | ALL, rows=49,504 + filesort | ref, rows=50 + Backward scan | **990×**, filesort 제거 |

> 자세한 분석은 [`docs/index-analysis.md`](docs/index-analysis.md)를 참고하세요.

---

## 프론트엔드 통합

React(CDN) + Tailwind CSS로 서버 빌드 의존 없이 프론트엔드를 구성했습니다. Spring Boot의 `static/` 디렉토리에서 직접 서빙합니다.

**규모**: 11개 JSX + index.html = 6,122줄, REST API 26건, STOMP 7개 함수

**주요 화면**: 메인 대시보드, 아티스트 홈, Fan Post/Artist Post/Fan Letter CRUD, HOT 탭(ScoreCursor + Offset), 라이브 채팅, DM, 래플 응모, 구독 관리, 프로필

---

## 로컬 실행 방법

### 사전 조건

- Java 21
- MySQL 8.x (port 3306)
- Redis 7.x (port 6379)

### 실행

```bash
# 1. DB 생성
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS infinite;"

# 2. Redis 기동
docker run -d --name redis -p 6379:6379 redis:7-alpine

# 3. 애플리케이션 실행
./gradlew bootRun
```

`application.yml`의 datasource 설정을 로컬 환경에 맞춰 조정하세요.

### 동시성 테스트 실행

```bash
# 테스트 DB 생성
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS infinite_test;"

# 테스트 실행 (-i 옵션으로 결과 출력 확인)
./gradlew test --tests "*.JellyConcurrencyTest" -i
```

---

## 프로젝트 구조

```
src/main/java/com/example/infinite/
├── domain/
│   ├── artistcontent/    # 게시글·댓글·리액션·해시태그·미디어 (176 files)
│   ├── dm/               # DM 룸·메시지 (13 files)
│   ├── member/           # 회원·아티스트·팔로우 (63 files)
│   ├── payment/          # 젤리·결제·빌링 (40 files)
│   ├── raffle/           # 래플·슬롯·Reservoir Sampling (34 files)
│   ├── realtimelive/     # 라이브 스트리밍·채팅 (22 files)
│   └── subscriptionmembership/  # 구독·멤버십 (12 files)
├── global/
│   ├── auth/             # JWT·Security·STOMP 인증
│   ├── common/           # 설정·DTO·캐시·QueryDSL 유틸
│   ├── error/            # 글로벌 예외 처리
│   ├── lock/             # 분산 락 (Redisson + Lettuce)
│   └── s3/               # 파일 업로드
└── resources/
    └── redis/            # Lua Scripts (entry.lua, close_slot.lua)
```

---

## 기술적 고민과 Trade-Off

프로젝트를 진행하면서 의도적으로 도입하지 않았거나, 미완성으로 남긴 항목들에 대한 근거를 정리합니다.

### 1. 캐싱 미적용 — 래플·DM·라이브 도메인

강혁 담당 3개 도메인에는 Redis 캐시(@Cacheable)를 적용하지 않았습니다.

**래플**: 응모 데이터는 이미 Redis(INCR + candidate)에서 처리되고, 조회(래플 목록/상세)는 아티스트당 수십 건 수준이라 DB 직접 조회로 충분합니다. 캐시를 넣으면 래플 상태 전이(PENDING → ACTIVE → COMPLETED) 시 무효화 로직만 복잡해집니다.

**DM**: 메시지는 실시간 STOMP로 전달되고, REST 조회는 커서 페이징으로 이력을 불러오는 용도입니다. 매번 다른 커서 값이 오므로 캐시 히트율이 극히 낮습니다.

**라이브 채팅**: 채팅 메시지는 인메모리 버퍼(ConcurrentLinkedQueue) → 배치 flush → DB 저장 구조로, 이미 읽기 경로에 DB를 타지 않습니다. LIVE 상태 검증은 Redis EXISTS로 대체하여 캐시의 필요가 없습니다.

### 2. 감사 로그 ACK-batchInsert 순서 트레이드오프

현재 `RaffleAuditConsumer`에서 `batchInsert()` 내부의 예외를 catch로 삼키고 있어, DB 저장에 실패해도 Redis 측 ACK가 진행됩니다. 이상적으로는 batchInsert 성공 후에만 ACK해야 하지만, Redis Streams Consumer Group의 재시도 메커니즘(PEL 관리)까지 구현하면 범위가 커지므로, 교육 프로젝트에서는 "로그 레벨 ERROR로 실패를 기록하고, 운영 환경에서는 DLQ 또는 재시도 로직이 필요하다"는 점을 인지한 상태로 남겨두었습니다.

### 3. `ddl-auto: validate` 환경에서의 `@Index` 반영

엔티티에 `@Index`를 선언하면 `ddl-auto: create/update` 환경에서는 자동 생성되지만, 프로덕션의 `validate` 모드에서는 스키마 변경이 일어나지 않습니다. 운영 환경에서는 Flyway나 Liquibase 같은 마이그레이션 도구가 필요하며, 이 사실을 인지하고 있습니다. 현재 프로젝트에는 마이그레이션 도구를 도입하지 않았는데, 제출 일정과 프로젝트 규모를 고려한 판단입니다.

### 4. Testcontainers 미사용

초기에 Testcontainers로 MySQL/Redis 컨테이너를 자동 기동하는 통합 테스트를 설계했으나, Docker Desktop 29.x와의 호환 이슈(Ryuk 컨테이너 실행 실패)로 로컬 컨테이너 방식으로 전환했습니다. `application-test.yml`에 사전 조건을 명시하고, README에 실행 환경 세팅 가이드를 포함하여 다른 팀원의 로컬 환경에서도 동작을 보장합니다.

### 5. Reservoir Sampling k>1 (`entry_multi.lua`) 미구현

현재 `entry.lua`는 슬롯당 1명(k=1) 전용입니다. carry-over(이월)로 이전 슬롯에서 당첨자가 없을 때 다음 슬롯의 k가 2 이상이 되는 시나리오에서는 `entry_multi.lua`(가중 Reservoir Sampling)가 필요하지만, 현재 구현에서는 carry-over 시 `target_winner_count`만 증가시키고 실제 다중 후보 관리는 하지 않습니다. k=1로도 MVP 시나리오는 충분히 커버되며, k>1 확장은 향후 과제로 남겨두었습니다.

### 6. k6 부하 테스트 미실시

CI/CD 파이프라인이 프로젝트 후반까지 불안정(ARM QEMU 에뮬레이션으로 빌드 16분, yml heredoc 이슈)했기 때문에, 배포 환경 기반 부하 테스트를 실시하지 못했습니다. 대신 로컬 환경에서 JUnit + `CyclicBarrier` 기반 동시성 테스트(100~10,000 스레드)와 EXPLAIN 기반 쿼리 성능 검증으로 대체했습니다. 도윤 씨 도메인에서는 10,000명 동시 좋아요 성공, 10만에서 타임아웃 — 단일 서버 한계 지점을 실측으로 확인했습니다.

---

## 참고 문서

- [`docs/concurrency-comparison.md`](docs/concurrency-comparison.md) — 동시성 제어 3종 비교 분석
- [`docs/index-analysis.md`](docs/index-analysis.md) — 인덱스 설계 및 EXPLAIN Before/After 비교
