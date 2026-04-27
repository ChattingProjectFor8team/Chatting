# Artist 검색/캐싱 분석

> 범위: `member.artist` 검색, 아티스트 상세, 인기 검색어

## 1. 검색 API 구조

### 검색 v1

- 메서드: `ArtistService.searchArtistsV1`
- 특징:
  - 원본 DB 조회
  - `LIKE` 검색
  - `page`는 받을 수 있지만 `size`는 서버에서 `10`으로 고정
  - 캐시 없음

### 검색 v2

- 메서드: `ArtistService.searchArtistsV2`
- 특징:
  - Caffeine 로컬 캐시
  - 캐시 키: 정규화된 `keyword + page`
  - `page`는 받을 수 있지만 `size`는 서버에서 `10`으로 고정
  - 같은 검색 반복 시 DB 조회를 줄인다

### 검색 v3

- 메서드: `ArtistService.searchArtistsV3`
- 특징:
  - Redis remote cache
  - 캐시 키: 정규화된 `keyword + page`
  - `page`는 받을 수 있지만 `size`는 서버에서 `10`으로 고정
  - 실제 서비스 기본 경로로 보는 검색 버전

## 2. 왜 v2와 v3를 둘 다 남겼는가

- 가장 직접적인 계기는 과제 요구사항이다.
- 과제에서는 `v1 원본`과 `v2 로컬 캐시`를 나눠 보여줄 필요가 있었다.
- 검색 결과는 같은 키워드가 반복 호출될 가능성이 높다
- 검색 결과는 완전한 실시간 일관성보다 짧은 stale 허용이 가능하다
- 다만 사용자가 `size`를 임의로 크게 늘리면 한 번의 요청과 캐시 엔트리가 과도하게 무거워질 수 있다
- v1을 남겨두고 v2를 추가하면
  - 캐시 없는 원본 경로
  - 캐시 적용 경로
  를 비교 설명하기 쉽다
- 다만 실제 서비스 관점에서는 로컬 캐시를 기본 선택지로 보기 어렵다.
- 대용량 트래픽을 받는 구조라면 보통 서버가 여러 대라고 보는 편이 자연스럽다.
- 로컬 캐시는 서버 간 공유가 안 되기 때문에, scale-out 환경에서 캐시 hit와 데이터 일관성이 서버마다 갈라진다.
- 그래서 실제 운영 환경을 먼저 생각하면 검색 캐시는 Redis 같은 remote cache가 더 일반적인 선택이다.
- 이번 프로젝트에서는 과제 비교 축을 위해 `v2(Caffeine)`를 남기고, 실사용 기본 경로는 `v3(Redis)`로 추가했다.

즉 검색은 "한 번에 얼마나 크게 볼 수 있는가"는 서버가 제한하고,
"다음 페이지로 더 볼 수 있는가"는 열어 둔 구조다.

## 3. 아티스트 상세 Redis 캐시

- 메서드: `ArtistService.getArtistV2`
- 캐시 이름: `artistDetailV2`
- 전략:
  - Cache-aside
  - 수정/삭제 시 `@CacheEvict`

이 경로는 로컬 캐시가 아니라 Redis 캐시라서,
검색 v2와는 다른 캐시 계층의 trade-off를 설명할 수 있다.

## 4. 인기 검색어

- 서비스: `ArtistSearchKeywordService`
- 저장소: `ArtistSearchKeywordRepository`
- 자료구조:
  - Redis ZSet
- 특징:
  - 동일 사용자 + 동일 키워드는 TTL 동안 1회만 집계
  - 공백/대소문자 차이를 정규화해 같은 키워드로 묶는다
  - 조회는 `offset` 기반으로 다음 묶음을 볼 수 있고, 한 번에 보는 크기는 `10개`로 고정한다

## 5. 테스트로 검증한 내용

- 테스트 클래스:
  - `ArtistSearchCachingIntegrationTest`
  - `ArtistSearchCachingBenchmarkIntegrationTest`

### 검증 항목

- 검색 v2는 캐시된 로컬 결과를 유지하고, v1은 최신 DB 상태를 즉시 반영한다
- 검색 v3는 Redis 캐시에 적재되고, 정규화된 `keyword + page` 키를 재사용한다
- 아티스트 상세 v2는 Redis 캐시에 적재되고, update 시 evict 후 최신 값으로 다시 적재된다
- 인기 검색어는 동일 사용자 중복 집계를 막고, 공백/대소문자를 정규화한다
- 검색은 `size 10` 고정으로 페이지를 넘겨 전체 결과를 볼 수 있다
- 인기 검색어도 `10개씩` offset을 넘겨 다음 묶음을 조회할 수 있다

## 6. 캐싱 성능 비교

측정 기준:

- 로컬 Docker 테스트 환경
- `artists` 더미데이터 `50,000건`
- 검색 키워드: `bts`
- 측정 항목:
  - `v1` 평균 응답시간: 캐시 없는 원본 조회 `30회 평균`
  - `v2 cold`: 캐시 비운 직후 첫 조회 `1회`
  - `v2 warm` 평균 응답시간: 캐시 적재 후 동일 조회 `30회 평균`
  - `v3 cold`: Redis 캐시 비운 직후 첫 조회 `1회`
  - `v3 warm` 평균 응답시간: Redis 캐시 적재 후 동일 조회 `30회 평균`

실측 결과:

| Scenario | Time |
| --- | ---: |
| `v1` average | `17.483ms` |
| `v2 cold` first call | `62ms` |
| `v2 warm` average | `0.087ms` |
| `v3 cold` first call | `21ms` |
| `v3 warm` average | `2.367ms` |
| `v1 -> v2 warm` improvement | `99.5%` |
| `v1 -> v3 warm` improvement | `86.5%` |

해석:

- `v2 cold`는 최초 1회 원본 조회 후 캐시에 적재하므로 오히려 느릴 수 있다.
- `v3 cold`도 마찬가지로 최초 1회는 원본 조회 후 Redis 적재 비용이 함께 든다.
- 동일 검색이 반복되는 `warm cache` 구간에서는 `v2`, `v3` 모두 DB를 다시 타지 않아 응답시간이 크게 줄었다.
- `v2`가 더 빠른 이유는 프로세스 내부 메모리라서 네트워크 왕복이 없기 때문이다.
- 하지만 이 차이는 단일 서버 기준이고, 여러 서버 운영에서는 `v3`처럼 공유 가능한 remote cache 쪽이 더 현실적인 선택이다.

주의:

- 이 수치는 로컬 단일 환경 기준이라 절대 수치보다 `v1 대비 v2 warm의 차이`를 보는 편이 맞다.
- scale-out 환경에서는 로컬 캐시 hit가 서버마다 갈라지므로, 실서비스 기본 선택지는 `v3`처럼 Redis remote cache 쪽이 더 자연스럽다.
- Redis warm 수치는 로컬 Docker 상태와 백그라운드 부하 영향을 꽤 받으므로, 절대 ms 값보다 `warm cache가 원본 조회보다 충분히 빨라지는가`를 보는 편이 안전하다.

## 7. 아티스트 상세 Redis 캐시 성능 비교

측정 기준:

- 로컬 Docker 테스트 환경
- 아티스트 1개 + 아티스트 멤버 8명
- 측정 항목:
  - `v1` 평균 응답시간: 캐시 없는 원본 조회 `30회 평균`
  - `v2 cold`: Redis 캐시 비운 직후 첫 조회 `1회`
  - `v2 warm` 평균 응답시간: Redis 캐시 적재 후 동일 조회 `30회 평균`

실측 결과:

| Scenario | Time |
| --- | ---: |
| `v1` average | `4.669ms` |
| `v2 cold` first call | `80ms` |
| `v2 warm` average | `1.144ms` |
| `v1 -> v2 warm` improvement | `75.5%` |

해석:

- 상세 조회는 검색보다 원본 쿼리 자체가 가벼워서 개선 폭이 검색만큼 크지는 않다.
- 그래도 같은 `artistId`가 반복 조회되는 구간에서는 Redis cache-aside 이득이 분명히 드러난다.
- 상세는 키가 `artistId` 하나라 eviction 대상도 명확해서, 검색보다 Redis 캐시 적용 설명이 더 단순하다.

## 8. 발표 포인트

- 왜 검색은 `v2(Caffeine)`와 `v3(Redis)`를 둘 다 남겼는가?
  - `v2`는 과제 필수의 local cache 비교 축
  - `v3`는 실서비스 기준 remote cache 기본 축
- 왜 상세는 Redis인가?
  - 상세는 키가 단순하고 eviction 대상이 명확해서 remote cache 적용 설명이 쉽다
- 왜 검색 결과 캐시는 즉시 전부 비우지 않았는가?
  - 검색 결과 키워드 조합이 많아 역산 eviction 비용이 크기 때문이다
  - 짧은 stale 허용 + TTL 만료가 더 단순하고 설명 가능하다
