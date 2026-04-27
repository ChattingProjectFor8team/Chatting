# Member / ArtistContent 인덱스 분석

> 범위: `member + artistcontent` 대표 조회 쿼리  
> 목적: 검색 LIKE 자체보다, 실제 커뮤니티 조회 경로에서 커서/정렬을 받쳐 주는 인덱스를 설명한다.

## 1. 왜 검색 LIKE를 인덱스 대표 사례로 잡지 않았는가

현재 검색 유틸은 `containsIgnoreCase`, 즉 `%keyword%` 성격의 `LIKE` 조건이다.

- 장점:
  - 사용자 경험상 부분 검색이 쉽다
- 한계:
  - 일반 B-Tree 인덱스로는 prefix 검색만큼 깔끔한 이점을 얻기 어렵다

그래서 인덱스 발표는 검색보다도,
실제로 트래픽이 많이 걸리는 커뮤니티 조회 쿼리 쪽을 대표 사례로 잡는 편이 더 설득력 있다.

## 2. 대표 쿼리와 인덱스 매핑

### A. FanPost 목록 커서 조회

- 쿼리 성격:
  - `WHERE artist_id = ? AND id < ? ORDER BY id DESC LIMIT ?`
- 인덱스:
  - `idx_fan_posts_artist_id_id`
- 엔티티:
  - `FanPost`

### B. ArtistPost 목록 커서 조회

- 쿼리 성격:
  - `WHERE artist_id = ? AND id < ? ORDER BY id DESC LIMIT ?`
- 인덱스:
  - `idx_artist_posts_artist_id_id`
- 엔티티:
  - `ArtistPost`

### C. 댓글 루트 조회

- 쿼리 성격:
  - `WHERE target_type = ? AND target_id = ? AND parent_id IS NULL AND id < ? ORDER BY id DESC LIMIT ?`
- 인덱스:
  - `idx_comments_target`
- 엔티티:
  - `Comment`

### D. 댓글 대댓글 조회

- 쿼리 성격:
  - `WHERE parent_id = ? ORDER BY id ASC LIMIT ?`
- 인덱스:
  - `idx_comments_parent`
- 엔티티:
  - `Comment`

### E. Follow 목록 조회

- 쿼리 성격:
  - `WHERE follower_member_id = ? ORDER BY id DESC LIMIT ?`
- 인덱스:
  - `idx_follows_follower_id_id`
- 엔티티:
  - `Follow`

## 3. 테스트로 검증한 내용

- 테스트 클래스:
  - `MemberArtistContentIndexExplainIntegrationTest`

### 검증 항목

- FanPost 목록 커서 조회는 `possible_keys`에 `idx_fan_posts_artist_id_id`가 잡히고, `type=ALL` 풀스캔이 아니다
- ArtistPost 목록 커서 조회는 `possible_keys`에 `idx_artist_posts_artist_id_id`가 잡히고, `type=ALL` 풀스캔이 아니다
- 댓글 루트 조회는 `possible_keys`에 `idx_comments_target`이 잡히고, `type=ALL` 풀스캔이 아니다
- 댓글 대댓글 조회는 `idx_comments_parent`를 사용한다
- Follow 목록 조회는 `idx_follows_follower_id_id` 또는 유니크 인덱스를 사용하며, `type=ALL` 풀스캔이 아니다

테스트에서는 `EXPLAIN` 결과의 `possible_keys`, `key`, `type`을 함께 본다.

- 커서 조회 쿼리는 작은 테스트 fixture에서는 옵티마이저가 `PRIMARY`를 선택할 수 있다
- 하지만 `possible_keys`에 우리가 설계한 복합 인덱스가 잡히는지,
  그리고 실제 접근 방식이 `ALL` 풀스캔이 아닌지는 안정적으로 검증할 수 있다
- 대댓글 조회와 Follow 조회는 조건 형태가 더 단순해서 의도한 보조 인덱스 또는 유니크 인덱스가 직접 선택된다

## 4. 발표 포인트

- 왜 `(artist_id, id)` 순서인가?
  - `artist_id`로 먼저 커뮤니티 범위를 좁히고,
  - 그 안에서 `id DESC` 커서 정렬을 이어가기 위해서다
- 왜 댓글은 인덱스를 둘로 나눴는가?
  - 루트 조회와 대댓글 조회의 조건 축이 다르기 때문이다
- 왜 인덱스를 너무 많이 늘리지 않았는가?
  - 조회는 빨라지지만, INSERT/UPDATE 비용은 늘어날 수 있기 때문이다

## 5. Before / After 응답시간 비교

측정 기준:

- 환경:
  - 로컬 Docker MySQL
  - benchmark DB: `springchatting_benchmark`
- 측정 방식:
  - `EXPLAIN ANALYZE`
  - 각 쿼리 5회 반복 후 평균
- 데이터셋:
  - `fan_posts` 100,000건
  - `artist_posts` 100,000건
  - `comments` 200,000건
  - `follows` 100,000건

| Query | Before (ms) | After (ms) | Improvement |
| --- | ---: | ---: | ---: |
| FanPost cursor | 0.814 | 0.234 | 71.3% |
| ArtistPost cursor | 0.724 | 0.205 | 71.7% |
| Comment root cursor | 0.566 | 0.172 | 69.6% |
| Comment replies | 18.540 | 0.094 | 99.5% |
| Follow list | 0.405 | 0.238 | 41.2% |

해석:

- `FanPost`, `ArtistPost`, `Comment root`는 모두 커서 조회에서 약 `70%` 전후 개선이 확인됐다
- `Comment replies`는 `parent_id` 조건이 없으면 거의 전 범위를 훑게 돼서,
  `idx_comments_parent` 효과가 가장 크게 드러났다
- `Follow list`는 before도 아주 느린 쿼리는 아니었지만,
  `(follower_member_id, id)` 인덱스로 정렬 비용까지 줄어 추가 개선이 확인됐다

## 6. 재측정 방법

스크립트:

- [measure-member-artistcontent-index-benchmark.ps1](/C:/java/assignment/springChatting/scripts/measure-member-artistcontent-index-benchmark.ps1)

실행:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\measure-member-artistcontent-index-benchmark.ps1
```

이 스크립트는 benchmark용 별도 테이블을 만들고,
같은 데이터셋에서 `before / after`를 각각 반복 측정해 Markdown 표 형태로 출력한다.
