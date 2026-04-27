# ArtistContent 동시성 제어 비교

> 범위: `member + artistcontent` 중 `ArtistPost 좋아요` 경로  
> 목적: 같은 멤버가 같은 글에 짧은 시간 안에 중복 요청을 보낼 때 정합성과 처리 방식을 비교한다.

## 1. 비교 대상

### A. 락 없는 core 경로

- 클래스: `ArtistPostLikeCoreService`
- 특징:
  - `Reaction` 조회 후 insert/delete
  - 분산 락 없음
  - 같은 멤버의 중복 토글이 동시에 들어오면 unique 충돌이나 race가 드러날 수 있다

### B. V1 — Lettuce 수동 락

- 클래스: `ArtistPostLikeLettuceV1Service`
- 락 키: `artist-post:like:{artistPostId}:member:{memberId}`
- 특징:
  - `SET NX + TTL + UUID + Lua unlock`을 직접 다룬다
  - 과제 필수인 Lettuce 기반 분산 락 설명 축
  - 동작 원리는 분명하지만 락 획득/해제 코드가 서비스에 직접 드러난다

### C. V2 — Redisson AOP 락

- 클래스: `ArtistPostLikeRedissonV2LockedService`
- 특징:
  - `@RedisLock` + AOP
  - 실제 토글 비즈니스는 `ArtistPostLikeCoreService`로 분리
  - 락 기술 코드와 도메인 코드가 더 깔끔하게 분리된다

### D. V3 — Redis Stream 비동기 write-path

- 요청 진입: `ArtistPostLikeRedissonV3LockedService`
- 비동기 반영: `ArtistPostLikeStreamProcessor`
- 특징:
  - 요청 경로는 `desired state`를 계산해 Stream command만 적재
  - 실제 DB 반영은 consumer가 수행
  - burst를 Redis Stream이 먼저 흡수하고, count는 flush/reconcile 경로가 맞춘다

## 2. 핵심 비교 포인트

### `no lock` vs `Lettuce v1`

- 같은 멤버의 중복 토글 경쟁에서
  - `no lock`: 예외가 발생할 수 있다
  - `Lettuce v1`: 직렬화되어 최종 상태가 안정적으로 남는다

### `Lettuce v1` vs `Redisson v2`

- 둘 다 같은 멤버 + 같은 글 단위로만 직렬화한다
- 차이:
  - V1은 락 구현 세부사항을 직접 관리
  - V2는 AOP로 분리해 유지보수성이 더 좋다

### `Redisson v2` vs `Stream v3`

- 이 비교는 락 비교가 아니라 write-path 비교다
- 차이:
  - V2: 동기 DB write
  - V3: 비동기 enqueue -> consumer DB write
- V3는 높은 트래픽에서 API 응답 경로를 더 가볍게 유지하는 방향이다

## 3. 테스트로 검증한 내용

- 테스트 클래스:
  - `ArtistPostLikeVersionComparisonIntegrationTest`

### 검증 항목

- 락 없는 core toggle은 같은 멤버의 동시 요청에서 실패가 발생할 수 있다
- Lettuce V1은 같은 멤버의 동시 요청을 직렬화하고 최종 like 상태를 일관되게 남긴다
- Redisson V2도 같은 조건에서 일관된 최종 상태를 남긴다
- Stream V3는 같은 멤버의 빠른 연속 요청에서도 pending state + consumer 처리 후 최종 의도 상태로 수렴한다

## 4. V3 고트래픽 수렴 테스트

- 테스트 클래스:
  - `ArtistPostTrafficConvergenceIntegrationTest`

이 테스트는 단순 락 비교가 아니라,
`ArtistPost like v3 + ArtistPost comment v2 + flush/read model`이 burst 이후 최종적으로 정합하게 수렴하는지를 본다.

### 기본 회귀 시나리오

- 좋아요 burst:
  - 기본값 `10,000`
  - 검증: 최종 `Reaction` 수와 `artist_posts.like_count`가 정확히 일치해야 한다
- 댓글 burst:
  - 기본값 `1,200`
  - 검증: 최종 활성 댓글 수와 `artist_posts.comment_count`가 정확히 일치해야 한다
- mixed read/write:
  - 기본값 `좋아요 1,500 + 댓글 700 + 읽기 2,000`
  - 검증:
    - 중간 읽기 응답 shape가 깨지지 않는다
    - 최종 `likeCount`, `commentCount`, 실제 원본 row 수가 모두 일치한다

### 기준 로컬 고부하 실험 결과

아래 수치는 `2026-04-24` 로컬 기준,
`@SpringBootTest + 로컬 Docker(MySQL/Redis) + 단일 앱 인스턴스` 환경에서 다시 돌린 결과다.

- 좋아요 `20,000`:
  - 통과
  - 약 `4분 48초`
- 좋아요 `30,000`:
  - 통과
  - 약 `7분 3초`
- 좋아요 `50,000`:
  - 실패
  - `600초` 안에 수렴하지 못함
- 좋아요 `100,000`:
  - 실패
  - `600초` 안에 수렴하지 못함
- 댓글 `12,000`:
  - 통과
  - 약 `4분 1초`
- mixed `10,000` 작업:
  - 좋아요 `4,000` + 댓글 `2,000` + 읽기 `4,000`
  - 통과
  - 약 `2분 4초`

### 해석 주의

- 이 수치는 AWS 운영 서버의 TPS/동접 한계가 아니다
- 현재 기준은 어디까지나
  - 로컬 머신
  - Docker MySQL/Redis
  - 단일 consumer
  - 단일 앱 인스턴스
  환경이다
- 안전하게 말할 수 있는 범위는:
  - 현재 구조는 로컬 기준으로도 큰 burst 이후 정합성이 수렴한다
  - 좋아요 경로의 시간 내 수렴 한계는 대략 `3만 ~ 5만` 사이 어딘가에 있다

### 재실행 방법

테스트는 JVM system property로 수치를 바꿔 다시 돌릴 수 있다.

- `-DartistPostTraffic.likeStormCount=30000`
- `-DartistPostTraffic.commentStormCount=12000`
- `-DartistPostTraffic.mixedLikeCount=4000`
- `-DartistPostTraffic.mixedCommentCount=2000`
- `-DartistPostTraffic.mixedReadCount=4000`
- `-DartistPostTraffic.convergenceTimeoutSeconds=600`

## 5. 발표 포인트

- 왜 락 키를 `post 전체`가 아니라 `member + post`로 잡았는가?
  - 같은 글에 1만 명이 눌러도 서로 다른 유저 요청은 병렬로 처리하기 위해서다
- 왜 V3가 필요한가?
  - V2도 정합성은 맞지만, 요청 경로에서 DB write를 직접 수행한다
  - V3는 burst를 queue에 먼저 흡수해 쓰기 경로를 더 유연하게 만든다
- 왜 `ArtistPostTrafficConvergenceIntegrationTest`가 중요한가?
  - 단순히 "락이 있다"가 아니라
  - `queue -> consumer -> flush -> read model` 전체 경로가 실제로 수렴하는지를 보여 주기 때문이다

## 6. 한계와 해석

- 과제 필수 축은 `Lettuce`이므로, 발표에서는 `no lock -> Lettuce 해결`을 먼저 설명해야 한다
- `Redisson v2`, `Stream v3`는 그 다음 단계의 진화 버전으로 설명하는 편이 자연스럽다
- `v3` 수렴 테스트는 매우 강한 증빙이지만, "Lettuce 락 실습" 자체를 대체하지는 않는다
