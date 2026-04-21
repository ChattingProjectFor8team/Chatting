# Frontend Team Vibe Handoff

작성일: 2026-04-21

이 문서는 같은 팀 프론트엔드 담당자가 AI 바이브 코딩에 바로 넣을 수 있게, 현재 백엔드 코드와 `HANDOFF_revised.md`를 기준으로 내 담당 범위의 구현 상태와 앞으로 구현할 것을 꼼꼼하게 정리한 문서다.

이 문서의 우선순위는 아래 순서로 본다.

1. 실제 백엔드 코드
2. `HANDOFF_revised.md`
3. `FRONTEND_ARTISTCONTENT_HANDOFF.md`
4. `src/main/resources/Connectfin-standalone.html`

`Connectfin-standalone.html`은 비주얼 톤 참고용이다. 실제 API, 데이터 구조, 권한, 예외, 미완성 상태는 이 문서를 기준으로 잡는 것이 안전하다.

## 1. 담당 범위 요약

내 담당 범위는 아래다.

- 메인 홈 일부
- 아티스트 상세 / 커뮤니티 헤더
- FanPost
- ArtistPost
- FanLetter
- Comment
- Hashtag
- Follow
- 공통 Media

지금 기준으로 상태를 한 줄로 요약하면 아래다.

- 실구현 완료: 아티스트 검색, 인기 검색어, 아티스트 상세, ArtistMember 관리, FanPost CRUD, FanPost 좋아요, FanPost 댓글/대댓글 조회, Hashtag 추천, FanPost 미디어 업로드 구조
- 부분 구현: ArtistPost 엔티티/리더/미디어 훅/댓글 경로 준비, FanLetter 엔티티, Follow 엔티티
- 미구현: ArtistPost 실API, ArtistPost 좋아요, FanLetter 실API, HOT 피드, Follow API, 메인 홈 대시보드 2종, 동시성 보강 및 검증 테스트

## 2. 가장 중요한 고정 정책

### 2-1. 인증 / 주체

- 로그인 주체는 `Member` 하나다.
- 아티스트도 별도 로그인 모델이 아니라 `Member`로 로그인한다.
- 역할 구분은 `MemberRole`로 한다.
- 현재 역할 enum은 `MEMBER`, `ARTIST`, `SUPER_ADMIN`이다.
- 프론트도 `일반 유저 계정`과 `아티스트 계정`을 완전히 다른 로그인 모델처럼 나누지 않는 것이 맞다.

### 2-2. 게시글 / 정렬 / HOT

- 커서 기준은 `id DESC`
- 무한 스크롤 기준
- HOT 기준은 최근 24시간 내 게시글
- HOT 정렬 점수는 `likeCount + commentCount`
- FanLetter는 댓글이 없으므로 실제로는 HOT 계산 시 `likeCount` 중심으로 동작하게 된다
- `likeCount`, `commentCount`, `mediaCount`는 엔티티 컬럼에 실시간 반영하고 나중에 배치로 보정할 수 있다

### 2-3. 댓글

- 최대 깊이 2
- 원댓글 = depth 1
- 대댓글 = depth 2
- 3뎁스 없음
- 3뎁스 대신 `@mention`
- 멘션은 대댓글에서만 실제 해석
- 멘션은 최대 1명만 해석
- 같은 루트 스레드 참여자 닉네임만 멘션 가능

### 2-4. 미디어

- FanPost / ArtistPost는 이미지 여러 장 또는 동영상 1개
- 이미지와 동영상 혼합 업로드 금지
- 이미지 최대 10장
- 동영상 최대 1개
- 기본 이미지 최대 용량 10MB
- 기본 동영상 최대 용량 100MB
- 허용 확장자
- 이미지: `jpg`, `jpeg`, `png`, `gif`, `webp`
- 동영상: `mp4`, `mov`, `webm`
- 사진 UI는 최대 6장까지 노출하고 6번째에 `+N` 오버레이 정책이 이미 핸드오프에 고정돼 있다
- FanLetter는 더 엄격하게 이미지 1장만 허용 방향이다

### 2-5. FanLetter

- 댓글 없음
- 이미지 중심
- 본문 텍스트는 핵심 아님
- 작성 권한은 구독 상태 조회가 붙어야 최종 확정된다
- 일반 좋아요는 가능
- 아티스트 special-like는 별도 persisted field로 저장하지 않고 `Reaction` 조회 결과를 DTO에서 조립하는 방향이다

### 2-6. Follow / 구독 배지

- Follow와 subscription badge는 다른 개념이다
- follow 표시와 구독 표시를 같은 UI 신호로 합치면 안 된다
- subscription 진실값은 `Member`가 아니라 구독 도메인에서 조회해야 한다

## 3. 현재 실제 구현 상태

## 3-1. 메인 홈 검색

실제 구현 완료 API:

- `GET /api/member/v1/artists/search`
- `GET /api/member/v2/artists/search`
- `GET /api/member/v1/artists/search/popular`

현재 응답 필드:

- 검색 결과: `id`, `name`, `slug`, `profileImageUrl`
- 인기 검색어: `keyword`, `score`

동작 정책:

- v1은 캐시 없음
- v2는 Caffeine 캐시
- 인기 검색어는 Redis ZSet
- 동일 사용자의 반복 검색은 TTL 동안 중복 집계 방지
- 구현은 되어 있지만 보안 설정상 로그인 후 사용하는 흐름으로 보는 편이 안전하다

프론트가 지금 바로 만들 수 있는 화면:

- 메인 홈 검색창
- 자동완성 없는 검색 결과 리스트
- 인기 검색어 랭킹 섹션

## 3-2. 아티스트 상세 / 커뮤니티 헤더

실제 구현 완료 API:

- `GET /api/member/v1/artists/{artistId}`
- `GET /api/member/v2/artists/{artistId}`

현재 응답 필드:

- `artistId`
- `name`
- `slug`
- `profileImageUrl`
- `coverImageUrl`
- `intro`
- `artistStatus`
- `createdAt`
- `artistMembers[]`

`artistMembers[]` 필드:

- `artistMemberId`
- `memberId`
- `stageName`
- `profileImageUrl`
- `status`
- `sortOrder`

정책:

- 상세 조회는 공개 GET
- slug는 영문/숫자/하이픈
- 아티스트 수정/삭제는 권한 제한

프론트가 지금 바로 만들 수 있는 화면:

- 커뮤니티 헤더
- 대표 프로필
- 커버 이미지
- 소개글
- 멤버 캐러셀 또는 멤버 리스트

## 3-3. ArtistMember 관리

실제 구현 완료 API:

- `POST /api/member/v1/artists`
- `PATCH /api/member/v1/artists/{artistId}`
- `DELETE /api/member/v1/artists/{artistId}`
- `POST /api/member/v1/artists/{artistId}/members`
- `PATCH /api/member/v1/artists/{artistId}/members/{artistMemberId}`
- `DELETE /api/member/v1/artists/{artistId}/members/{artistMemberId}`

이 영역은 일반 사용자용 공개 UI보다 관리자성/아티스트 관리성 UI에 가깝다. 프론트 메인 커뮤니티 화면 우선순위에서는 낮지만, 아티스트 계정용 관리 화면이 필요하면 이미 실API가 있다.

## 3-4. FanPost

실제 구현 완료 API:

- `POST /api/post/v1/artists/{artistId}/fan-posts`
- `GET /api/post/v1/artists/{artistId}/fan-posts`
- `GET /api/post/v1/artists/{artistId}/fan-posts/{fanPostId}`
- `PATCH /api/post/v1/artists/{artistId}/fan-posts/{fanPostId}`
- `DELETE /api/post/v1/artists/{artistId}/fan-posts/{fanPostId}`
- `POST /api/post/v1/artists/{artistId}/fan-posts/{fanPostId}/likes/toggle`
- `POST /api/post/v1/artists/{artistId}/fan-posts/{fanPostId}/comments`
- `DELETE /api/post/v1/artists/{artistId}/fan-posts/{fanPostId}/comments/{commentId}`
- `GET /api/post/v1/artists/{artistId}/fan-posts/{fanPostId}/comments/{commentId}/replies`

FanPost 리스트 카드 필드:

- `fanPostId`
- `artistId`
- `writerId`
- `writerNickname`
- `writerProfileImageUrl`
- `fanMembershipSubscribed`
- `dmSubscribed`
- `content`
- `likeCount`
- `commentCount`
- `mediaCount`
- `media[]`
- `hashtags[]`
- `createdAt`

`media[]` 필드:

- `mediaId`
- `mediaType`
- `fileUrl`
- `thumbnailUrl`
- `sortOrder`

FanPost 상세 추가 필드:

- `comments`

`comments`는 `CursorSliceResponse<CommentResponse>`다.

FanPost 현재 정책:

- 리스트 공개 조회 가능
- 무한 스크롤
- size 10
- `id DESC`
- 상세에서만 루트 댓글 포함
- 댓글 루트 목록 size 20
- 루트 댓글 정렬은 `id DESC`
- 대댓글은 별도 API 온디맨드 조회
- 대댓글 정렬은 `id ASC`
- 수정/삭제는 작성자 본인만 가능
- soft delete

FanPost 작성/수정 현재 정책:

- `multipart/form-data`
- JSON body 전용 아님
- `content`와 `files`를 한 요청에서 같이 보낸다
- 본문만 있는 글 가능
- 파일만 있는 글 가능
- 둘 다 비어 있으면 불가
- 수정 시 `content == null`이면 기존 본문 유지
- 수정 시 `files` 파라미터를 보내면 기존 첨부 전체 교체
- 수정 시 `files`를 아예 안 보내면 기존 첨부 유지

FanPost 좋아요 현재 정책:

- 로그인 필요
- 토글 방식
- 현재 응답은 아래 구조다

```json
{
  "success": true,
  "data": {
    "targetId": 10,
    "reacted": true,
    "reactionCount": 31
  },
  "error": null
}
```

FanPost 댓글 현재 정책:

- 댓글 수정은 아직 없음
- 삭제된 원댓글에 자식이 남아 있으면 `"삭제된 댓글입니다."` placeholder 유지
- 자식 없는 원댓글은 soft delete 후 숨김
- placeholder 부모의 마지막 대댓글도 사라지면 부모도 최종 삭제
- 작성자 옆 구독 배지 boolean은 현재 실제 subscription 조회 결과가 내려온다

프론트가 지금 바로 실연동 가능한 화면:

- FanPost 리스트
- FanPost 상세
- FanPost 작성
- FanPost 수정
- FanPost 삭제
- FanPost 좋아요 버튼
- FanPost 댓글 작성/삭제
- 대댓글 펼침 조회

## 3-5. Comment 공통

현재 댓글 응답 필드:

- `commentId`
- `parentCommentId`
- `depth`
- `writerId`
- `writerNickname`
- `writerProfileImageUrl`
- `fanMembershipSubscribed`
- `dmSubscribed`
- `content`
- `mentionedMember`
- `replyCount`
- `replies`
- `createdAt`

`mentionedMember` 필드:

- `memberId`
- `nickname`
- `profileImageUrl`

중요한 UI 해석:

- 루트 댓글의 `replies`는 기본적으로 빈 배열로 생각하는 편이 안전하다
- 실제 대댓글은 별도 API 호출 후 붙인다
- 루트 댓글에는 `replyCount`가 들어온다
- depth 2 입력창에서만 멘션 UX를 주면 충분하다

## 3-6. Hashtag

실제 구현 완료 API:

- `GET /api/post/v1/hashtags/suggestions?keyword=...&limit=...`

현재 응답 필드:

- `hashtagName`
- `usageCount`

동작 정책:

- 전역 추천
- artistId 의존 안 함
- FanPost create/update/delete에 이미 연동됨
- 비어 있는 키워드면 상위 인기 태그를 내려주는 구조

프론트가 지금 바로 실연동 가능한 화면:

- FanPost 작성 중 해시태그 자동완성
- 해시태그 추천 드롭다운

## 3-7. Media 인프라

현재 상태:

- 별도 public media controller 없음
- 게시글 작성/수정 flow 안에서 같이 처리
- 공통 `Media` 엔티티 사용
- `ObjectStorageClient`, `S3ObjectStorageClient`, `MediaService` 구조 있음
- FanPost는 실연동 완료
- ArtistPost용 attach/replace/delete 메서드도 이미 `MediaService`에 준비돼 있음

중요한 운영 주의:

- 로컬 기본 설정은 `media.storage.enabled: false`
- 즉 백엔드 코드상 업로드 구조는 있지만, 환경 설정이 안 되면 실제 업로드는 `MEDIA_STORAGE_NOT_CONFIGURED`로 실패할 수 있다
- 프론트는 업로드 UI를 먼저 만들 수 있지만, 로컬 실테스트 가능 여부는 스토리지 설정 여부에 달려 있다

## 4. 부분 구현 상태

## 4-1. ArtistPost

현재 실제 코드 상태:

- `ArtistPost` 엔티티 있음
- `ArtistPostReader` 있음
- `ArtistPostRepository` / `Custom` / `Impl` 파일은 있으나 실쿼리 구현은 사실상 비어 있음
- `ArtistPostController` 비어 있음
- `ArtistPostService` 비어 있음
- `ArtistPostRequest` / `ArtistPostResponse` 비어 있음
- 댓글 컨트롤러에는 아래 artist-post 댓글 경로가 이미 존재함
- `POST /api/post/v1/artists/{artistId}/artist-posts/{artistPostId}/comments`
- `DELETE /api/post/v1/artists/{artistId}/artist-posts/{artistPostId}/comments/{commentId}`
- `GET /api/post/v1/artists/{artistId}/artist-posts/{artistPostId}/comments/{commentId}/replies`

즉 현재 해석은 아래가 맞다.

- 댓글 공통 구조는 ArtistPost 재사용 준비가 되어 있음
- 하지만 정작 ArtistPost 본체 API가 없어서 프론트 실연동 대상은 아직 아님
- 화면은 FanPost 기반으로 목업 가능

프론트가 지금 해두면 좋은 것:

- ArtistPost 리스트 화면 목업
- ArtistPost 상세 화면 목업
- ArtistPost 작성 폼 목업
- ArtistPost 카드 컴포넌트
- 아티스트 인증 뱃지 UI
- 작성 멤버 stage name / 그룹명 병기 UI

## 4-2. FanLetter

현재 실제 코드 상태:

- `FanLetter` 엔티티 있음
- `FanLetterRepository` / `Custom` / `Impl` 파일은 있으나 실구현 없음
- `FanLetterController` 비어 있음
- `FanLetterService` 비어 있음
- `FanLetterRequest` / `FanLetterResponse` 비어 있음
- 보안 설정에는 `/api/post/v1/fan-letters/**` 인증 필요 경로가 이미 걸려 있음

즉 현재 해석은 아래가 맞다.

- 도메인 테이블 방향은 잡혀 있음
- 실제 프론트 연동 가능한 API는 아직 없음
- 화면은 mock 우선이 맞다

프론트가 지금 해두면 좋은 것:

- 이미지 중심 카드형 리스트
- FanLetter 상세 모달 또는 상세 페이지
- FanLetter 작성 모달
- 댓글 UI 제거
- special-like 배지 자리 확보

## 4-3. Follow

현재 실제 코드 상태:

- `Follow` 엔티티 있음
- 컬럼 구조는 `fromMember`, `toMember` 기반 member-to-member follow다
- `FollowController`, `FollowService`, `FollowRequest`, `FollowResponse`는 비어 있음

즉 현재 해석은 아래가 맞다.

- follow 모델은 member 기반으로 굳어졌다
- artist follow와 artist-member follow 표시를 프론트에서는 다른 UX로 분리하는 것이 맞다
- 실제 버튼 연동 API는 아직 없다

## 4-4. 구독 배지

현재 실제 코드 상태:

- FanPost 응답에 `fanMembershipSubscribed`, `dmSubscribed` 필드가 이미 있다
- 댓글 응답에도 `fanMembershipSubscribed`, `dmSubscribed` 필드가 추가되었다
- 실제 값은 subscription 도메인 batch 조회 결과를 서비스에서 조립해 내려준다

프론트 해석:

- 배지 UI는 먼저 만들어도 된다
- FanPost / 댓글 영역은 실제 값 기준으로 처리해도 된다
- FanLetter 쪽은 별도 구현 시점에 다시 확인하면 된다

## 4-5. 메인 홈 대시보드 2종

현재 방향만 확정되어 있고 API는 없다.

Dashboard A:

- 팔로우한 아티스트별 최신 ArtistPost 2개

Dashboard B:

- 팔로우한 아티스트 멤버 기준 최신 ArtistPost 2개

프론트 해석:

- 둘은 한 탭 안의 변형이 아니라 별도 섹션이 맞다
- 둘 다 ArtistPost 카드 재사용 가능
- 지금은 mock data로 먼저 만드는 것이 맞다

## 5. 앞으로 내가 구현할 것

아래는 내가 다음으로 진행할 예정인 일이고, 프론트가 무엇을 준비해야 하는지도 같이 적는다.

## 5-1. ArtistPost

백엔드 예정 작업:

- create/update/delete
- list/detail
- FanPost와 같은 cursor slice
- 댓글 공통 구조 재사용
- 같은 미디어 attachment flow 재사용
- 권한 체크
- artist 소속 멤버 검증

프론트 준비 포인트:

- FanPost와 동일한 큰 레이아웃으로 재사용
- 단, 작성자 영역은 별도 설계 필요
- 그룹 아티스트명
- 실제 작성 멤버명 또는 stage name
- 아티스트 인증 표시
- 댓글 depth 2 정책은 그대로 재사용 가능
- 작성/수정 폼도 FanPost와 거의 같은 UX로 가되, 권한 가드가 더 강하게 들어간다

권장 mock 필드:

- `artistPostId`
- `artistId`
- `writerId`
- `writerName`
- `writerStageName`
- `writerProfileImageUrl`
- `artistVerified`
- `content`
- `likeCount`
- `commentCount`
- `media[]`
- `hashtags[]`
- `createdAt`

## 5-2. ArtistPost 좋아요

백엔드 예정 작업:

- toggle 방식 유력
- 대용량 트래픽 고려
- 내 담당 범위에서 동시성 과제 후보 1순위

프론트 준비 포인트:

- API를 add/remove 분리형으로 가정하지 말고 toggle 버튼 1개 기준으로 설계
- optimistic update를 너무 강하게 고정하지 말고 서버 count 재동기화가 가능한 구조가 좋다
- 좋아요 애니메이션은 가능하지만 count는 최종적으로 서버 응답 기준으로 덮어쓰는 편이 안전하다

## 5-3. FanLetter

백엔드 예정 작업:

- create/update/delete/list/detail
- 일반 좋아요
- 아티스트 special-like 응답
- 작성 권한은 subscription 질의 연동 필요

프론트 준비 포인트:

- 텍스트형 커뮤니티 포스트처럼 만들면 안 된다
- 이미지 중심 카드형 레이아웃
- 댓글 영역 제거
- 작성 버튼은 구독 여부에 따라 노출/disabled 처리될 가능성이 높다
- special-like 뱃지나 스탬프 자리 확보
- 좋아요는 가능하지만 댓글 카운트 UI는 필요 없다

권장 mock 필드:

- `fanLetterId`
- `artistId`
- `writerId`
- `writerNickname`
- `writerProfileImageUrl`
- `imageUrl`
- `likeCount`
- `artistLiked`
- `artistProfileImageUrl`
- `createdAt`

## 5-4. HOT 게시글 필터

백엔드 예정 작업:

- 대상은 `FanPost + FanLetter`
- 최근 24시간 제한
- 정렬 기준은 `likeCount + commentCount`

프론트 준비 포인트:

- HOT 필터는 별도 정렬 탭 또는 토글로 분리하는 편이 안전하다
- 24시간 제한 문구가 UI에 보여야 한다
- FanLetter는 댓글이 없으므로 HOT 점수 해석이 FanPost와 완전히 같지 않다
- 따라서 혼합 피드라면 카드 타입을 공통 래퍼 + 내부 variant 구조로 잡는 것이 좋다

권장 화면 방향:

- `최신` / `HOT 24h` 분리
- FanPost 카드와 FanLetter 카드를 같은 피드에 섞을 수 있게 컴포넌트 분기

## 5-5. 댓글/좋아요 카운터 동시성

백엔드 예정 작업:

- FanPost
- ArtistPost
- FanLetter
- 특히 `likeCount`, `commentCount`
- 이후 동시성 테스트도 추가

프론트 준비 포인트:

- 카운터를 절대 프론트 단독 source of truth로 두지 말 것
- 여러 탭/여러 사용자 동시 반영을 고려해 재조회 또는 응답 덮어쓰기 구조가 필요
- optimistic UI는 가능하지만 롤백 처리 경로가 있어야 한다

## 6. 프론트가 바로 써먹는 API 계약

## 6-1. 공통 성공 응답

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

## 6-2. 공통 실패 응답

```json
{
  "success": false,
  "data": null,
  "error": {
    "timestamp": "2026-04-21T10:00:00",
    "status": 400,
    "error": "BAD_REQUEST",
    "code": "M002",
    "message": "지원하지 않는 파일 형식입니다.",
    "path": "/api/post/v1/artists/1/fan-posts"
  }
}
```

## 6-3. CursorSliceResponse

```json
{
  "success": true,
  "data": {
    "content": [],
    "nextCursor": 120,
    "hasNext": true,
    "size": 10
  },
  "error": null
}
```

## 6-4. PageResponse

검색 API 일부는 아래처럼 `ApiResponse` 래퍼 없이 `PageResponse` 자체가 바로 내려온다.

```json
{
  "content": [],
  "number": 1,
  "size": 10,
  "totalPages": 5,
  "totalElements": 42,
  "isLast": false
}
```

## 6-5. FanPost 작성 요청 예시

`multipart/form-data`

필드:

- `content`
- `files`

해석:

- `files`는 여러 개 가능
- 작성은 본문만 또는 파일만 가능

## 6-6. FanPost 수정 요청 예시

`multipart/form-data`

필드:

- `content`
- `files`

해석:

- `content`가 없으면 기존 본문 유지
- `files`를 보내면 기존 첨부 전체 교체
- `files`를 아예 안 보내면 기존 첨부 유지

## 6-7. FanPost 상세의 댓글 흐름

추천 프론트 흐름:

1. 상세 진입 시 FanPost 상세 API 호출
2. 루트 댓글만 먼저 렌더
3. 특정 댓글의 `replyCount`가 있으면 `답글 보기` 버튼 노출
4. 버튼 클릭 시 replies API 호출
5. 해당 댓글 하위에 답글 삽입 렌더

## 7. 로그인 / 권한 매트릭스

비로그인 공개 조회 가능:

- 아티스트 상세 v1/v2
- FanPost 리스트
- FanPost 상세
- FanPost 대댓글 조회
- Hashtag 추천

로그인 필요:

- 아티스트 검색 v1/v2
- 인기 검색어
- FanPost 작성
- FanPost 수정/삭제
- FanPost 좋아요
- FanPost 댓글 작성/삭제
- FanLetter 전체 예정

아티스트 권한 필요:

- 아티스트 생성
- ArtistMember 관리
- ArtistPost 작성 예정

중요:

- 보안 설정에 ArtistPost GET 공개 경로가 미리 열려 있지만 실제 ArtistPost controller가 없으므로 지금 당장 쓸 수 있는 공개 API라고 보면 안 된다
- FanLetter도 보안 경로는 걸려 있지만 controller가 없으므로 아직 실API 아님

## 8. 프론트 화면 우선순위 추천

실API 연동 우선:

1. 메인 홈 검색
2. 아티스트 상세 헤더
3. FanPost 리스트
4. FanPost 상세
5. FanPost 작성/수정
6. Hashtag 자동완성

mock 우선:

1. ArtistPost 리스트/상세/작성
2. FanLetter 리스트/상세/작성
3. 메인 홈 대시보드 2종
4. Follow 버튼 / 팔로우 섹션

## 9. 프론트가 특히 조심해야 할 것

- FanPost 작성/수정은 JSON API처럼 만들면 안 되고 `multipart/form-data`여야 한다
- FanPost 수정에서 `files`를 보내면 부분 추가가 아니라 전체 교체다
- 댓글은 depth 3 구조를 만들 필요 없다
- replies는 상세 응답에 전부 들어오는 구조가 아니다
- subscription badge 필드는 아직 실데이터로 믿으면 안 된다
- ArtistPost 댓글 경로는 보이지만 ArtistPost 본체가 아직 없으니 실사용 불가다
- FanLetter는 텍스트 피드처럼 만들면 정책과 어긋난다
- HOT은 단순 인기순이 아니라 최근 24시간 제한이 붙는다
- 로컬 환경에서는 media storage가 기본 비활성화라 업로드 테스트가 실패할 수 있다

## 10. 프론트 AI에 넘기기 좋은 설명

아래 문장을 그대로 써도 된다.

> `Connectfin-standalone.html`은 비주얼 레퍼런스로만 참고하고, 실제 기능 범위와 데이터 구조는 `FRONTEND_TEAM_VIBE_HANDOFF.md`를 기준으로 작업해줘. 실연동 가능한 것은 메인 홈 검색, 아티스트 상세 헤더, FanPost 리스트/상세/작성/수정/좋아요/댓글이고, ArtistPost/FanLetter/대시보드/Follow는 아직 백엔드 미구현이라 mock 우선으로 설계해줘. FanPost는 반드시 multipart 작성/수정, cursor infinite scroll, 댓글 depth 2, 대댓글 별도 조회 구조를 반영해줘.

## 11. 최종 요약

지금 프론트가 가장 안정적으로 붙일 수 있는 실구현 영역은 `검색 + 아티스트 상세 + FanPost 전체 + Hashtag 추천`이다.

앞으로 내가 구현할 핵심은 아래다.

- ArtistPost
- ArtistPost 좋아요 대용량 트래픽 대응
- FanLetter
- FanPost + FanLetter HOT 24시간 필터
- 포스트 좋아요/댓글 카운터 동시성 보강

즉 프론트는 지금 당장은 `FanPost 실연동`, 그다음은 `ArtistPost/FanLetter/HOT/dashboard/follow 목업` 순서로 가는 것이 가장 안전하다.
