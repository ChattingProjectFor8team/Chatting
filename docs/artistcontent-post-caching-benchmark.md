# ArtistContent Post 캐시 벤치마크

> 범위: `ArtistPost base cache`, `post hot data cache`

## 1. 왜 이 캐시들을 먼저 측정했는가

- 댓글 캐시는 admission 조건이 끼어 있어서, 같은 입력으로도 "캐시에 올라가느냐"가 먼저 갈린다.
- 그래서 이번 벤치마크에서는 admission이 없는 캐시만 먼저 골랐다.
- 대표 대상은 아래 두 가지다.
  - `ArtistPost base cache`
  - `post hot data cache`

이 두 개를 보면 현재 설계의 핵심인 `base/hot 분리`가 실제로 어떤 이득을 주는지 비교적 깔끔하게 설명할 수 있다.

## 2. 측정 대상

### ArtistPost base list cache

- 메서드: `ArtistPostBaseCacheService.getArtistPostBaseSlice`
- 캐시 이름: `artistPostListBase`
- 의미:
  - 목록 응답에서 본문, 작성자, 미디어 preview, 해시태그만 길게 캐시
  - 좋아요 수, 댓글 수는 여기 섞지 않음

### ArtistPost base detail cache

- 메서드: `ArtistPostBaseCacheService.getArtistPostBaseDetail`
- 캐시 이름: `artistPostDetailBase`
- 의미:
  - 상세 응답에서 본문, 작성자, 전체 미디어, 해시태그만 길게 캐시
  - count와 댓글은 별도 영역

### post hot data cache

- 메서드: `PostHotDataCacheService.getPostHotDataMap`
- 캐시 이름: `postHotData`
- 의미:
  - `likeCount`, `commentCount`만 post별로 따로 캐시
  - list와 detail이 같은 hot key를 공유

## 3. 측정 기준

- 로컬 Docker 테스트 환경
- 아티스트 1명
- ArtistPost `200건`
- hot data 대상 post `10건`
- 측정 횟수 `30회 평균`

시나리오는 아래 3개로 통일했다.

- `uncached`
  - 캐시를 타지 않는 원본 로더 직접 호출 평균
- `cold`
  - 캐시를 비운 직후 첫 조회 1회
- `warm`
  - 캐시 적재 후 동일 조회 30회 평균

## 4. 실측 결과

벤치마크 테스트:

- `ArtistPostRedisCacheBenchmarkIntegrationTest`

재실행 스크립트:

- `scripts/measure-artist-post-cache-benchmark.ps1`

실측 결과는 테스트 실행 후 아래 표에 반영한다.

| Scenario | Time |
| --- | ---: |
| `base list uncached` | `9.992ms` |
| `base list cold` | `2ms` |
| `base list warm` | `1.408ms` |
| `base detail uncached` | `7.226ms` |
| `base detail cold` | `1ms` |
| `base detail warm` | `0.886ms` |
| `hot data uncached` | `5.786ms` |
| `hot data cold` | `14ms` |
| `hot data warm` | `7.230ms` |

## 5. 해석 포인트

- `base cache`는 본문/미디어/해시태그 같은 조립 비용을 다시 내지 않게 해 준다.
- `hot cache`는 count만 짧은 TTL로 따라가게 만들어, 자주 안 바뀌는 구조 데이터를 같이 흔들지 않게 해 준다.
- `cold`가 `uncached`보다 느릴 수도 있다.
  - Redis 적재 비용이 한 번 같이 들어가기 때문이다.
- 중요한 비교는 `uncached -> warm`이다.
  - 반복 조회가 있을 때 DB와 조립 비용을 얼마나 줄였는지 보는 쪽이 맞다.
- 이번 측정에서는 `base list`, `base detail`은 warm cache가 분명히 더 빨랐다.
- 반면 `hot data`는 로컬 10건 마이크로벤치마크에서는 warm cache가 오히려 더 느렸다.
  - 이유는 DB에서 count 10건만 읽는 쿼리가 이미 매우 싸고,
  - Redis 네트워크 왕복 + 직렬화 비용이 그보다 크게 잡혔기 때문이다.
- 즉 `hot cache`의 목적은 "무조건 더 빠른 단건 응답"보다
  - count를 base 응답에서 분리하고,
  - list/detail이 같은 count key를 재사용하고,
  - flush 주기와 짧은 TTL을 맞추기 쉽게 만드는 쪽에 더 가깝다.
- 로컬 Redis 벤치마크는 배경 부하에 따라 수치가 조금 흔들릴 수 있으므로, 절대값보다 `warm에서 어떤 경향이 나오는지`를 보는 편이 안전하다.

## 6. 발표 때 어떻게 설명하면 좋은가

- "게시글 전체 응답을 한 덩어리로 짧게 캐시하지 않고, base와 hot을 나눴다"
- "본문/미디어/해시태그는 길게, like/comment count는 짧게 가져갔다"
- "그래서 count 때문에 전체 조립 결과를 자주 버리지 않아도 된다"
- "실측상 warm cache 구간에서 이 분리 구조의 이득이 확인됐다"
