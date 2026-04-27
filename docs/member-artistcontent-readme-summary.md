# Member + ArtistContent README용 축약본

이 문서는 `member + artistcontent` 파트를 `README`나 포트폴리오 소개 문서에 넣기 좋게 짧게 정리한 버전이다.
세부 설계 의도와 trade-off는 [`member-artistcontent-design-rationale.md`](member-artistcontent-design-rationale.md)에서 따로 설명한다.

## 1. 검색 / 아티스트

- 아티스트 검색은 `v1 원본 조회`, `v2 Caffeine 로컬 캐시`, `v3 Redis 캐시`로 분리했다.
- 과제 비교용 캐시 축은 `v2`지만, 실사용 기본 경로는 여러 서버에서 공유 가능한 `v3 Redis`로 두었다.
- 인기 검색어는 검색 결과 캐시와 분리해 `Redis ZSet`으로 집계하고, 아티스트 상세는 `Redis cache-aside`로 최적화했다.

## 2. 게시글 / 댓글

- `FanPost`, `ArtistPost`, `FanLetter`는 `base cache + hot cache` 구조로 나눠 본문과 반응 수의 수명을 분리했다.
- 댓글은 `base/hot` 대신 짧은 TTL의 whole-response 캐시를 조건부로 두고, root 댓글과 reply의 조회 패턴을 다르게 처리했다.
- 댓글 구조는 depth 2까지만 허용하고, 그 이후 대화는 같은 스레드 안의 `@mention`으로 대체했다.

## 3. ArtistPost 동시성 / 비동기 처리

- 모든 게시글에 같은 복잡도를 적용하지 않고, 트래픽이 가장 몰릴 가능성이 큰 `ArtistPost`만 Redis Stream 기반 비동기 write-path를 사용했다.
- 좋아요/댓글 count는 요청 경로에서 바로 DB를 두드리지 않고, stream 소비와 짧은 주기 flush를 거쳐 최종 수렴하도록 설계했다.
- `no lock / Lettuce v1 / Redisson v2 / Stream v3` 비교와 별도 수렴 테스트로 동시성보다 `write-path 정합성`을 더 강하게 검증했다.

## 4. 홈 / Follow / YouTube

- 아티스트 홈과 메인 홈은 하나의 거대한 피드가 아니라, 여러 섹션을 조립하는 오케스트레이션 API로 설계했다.
- Follow는 일반 SNS 확장형 모델 대신 `Member -> ArtistMember` 최소 모델만 남겨 홈 개인화에 집중했다.
- YouTube 탭은 파일 업로드가 아니라 외부 링크 아카이브로 분리했고, 작성자 메타데이터는 snapshot으로 저장해 과거 카드 표시를 안정적으로 유지했다.

## 5. FanLetter / 미디어 정책

- FanLetter는 팬 멤버십 구독자 전용, 댓글 없음, 이미지 1장 정책으로 FanPost와 역할을 분리했다.
- 아티스트 좋아요는 "누가 눌렀는지" 세부 멤버보다 "아티스트가 반응했다"는 대표 정보만 노출하도록 정리했다.
- ArtistPost와 FanPost 목록 미디어는 전체 대신 `preview 6개`만 내려 응답 크기와 UI 규칙을 맞췄다.

## 6. 조회 전략 / 인덱스

- 최신순 목록은 `offset`보다 `cursor`를 우선했고, HOT처럼 정렬 축이 다른 곳은 그 축에 맞는 커서를 따로 설계했다.
- 메인 홈 구독 섹션은 "전체 최신 글 몇 개"가 아니라 "artist별 최신 N개"를 한 번에 읽는 전용 query로 조립했다.
- 인덱스 최적화는 별도 문서에서 `member + artistcontent` 조회 경로를 중심으로 Before / After EXPLAIN과 응답시간 비교까지 정리했다.

## 문서 역할 분리

- README / 포트폴리오 본문: 이 문서
- 설계 의도 / trade-off / 구현 근거: [`member-artistcontent-design-rationale.md`](member-artistcontent-design-rationale.md)
- 인덱스 근거: [`member-artistcontent-index-analysis.md`](member-artistcontent-index-analysis.md)
