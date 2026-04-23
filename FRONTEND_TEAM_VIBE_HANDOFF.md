# Frontend Team Vibe Handoff

작성일: 2026-04-23

이 문서는 같은 팀 프론트엔드 담당자가 AI 바이브 코딩에 바로 넣을 수 있게, 현재 백엔드 코드와 `HANDOFF_revised.md`를 기준으로 내 담당 범위의 구현 상태와 앞으로 구현할 것을 꼼꼼하게 정리한 문서다.

이 문서의 우선순위는 아래 순서로 본다.

1. 실제 백엔드 코드
2. `HANDOFF_revised.md`
3. `FRONTEND_ARTISTCONTENT_HANDOFF.md`
4. `src/main/resources/Connectfin-standalone.html`

`Connectfin-standalone.html`은 비주얼 톤 참고용이다. 실제 API, 데이터 구조, 권한, 예외, 미완성 상태는 이 문서를 기준으로 잡는 것이 안전하다.

중요 메모:

- 현재 백엔드 사용자는 구현을 우선 끝내는 단계라서, 최근 추가된 기능들을 아직 직접 충분히 검토하거나 공부하지 않았다
- 따라서 이 문서는 "사용자가 이미 세부 구현을 다 숙지했다"는 전제로 읽지 말고, 실제 코드와 주석을 다시 따라가며 확인해야 한다
- 특히 메인 홈 대시보드, Follow, YouTube, VOD는 먼저 붙여 둔 뒤 나중에 사용자가 직접 공부/리뷰할 예정인 기능들이다

## 1. 담당 범위 요약

내 담당 범위는 아래다.

- 메인 홈 일부
- 아티스트 상세 / 커뮤니티 헤더
- 아티스트 홈 대시보드
- FanPost
- ArtistPost
- FanLetter
- Comment
- Hashtag
- Follow
- 공통 Media

지금 기준으로 상태를 한 줄로 요약하면 아래다.

- 실구현 완료: 아티스트 검색, 인기 검색어, 아티스트 상세, 아티스트 홈 대시보드, ArtistMember 관리, FanPost CRUD, FanPost 좋아요, FanPost 댓글/대댓글 조회, FanPost HOT, ArtistPost CRUD, ArtistPost 좋아요, ArtistPost 댓글/대댓글 조회, FanLetter CRUD, FanLetter 좋아요, FanLetter HOT, Hashtag 추천, FanPost/ArtistPost/FanLetter 미디어 업로드 구조, 실시간 스트리밍 메타데이터/채팅 기본 구조
- 실구현 완료 추가: 메인 홈 대시보드, ArtistMember 전용 Follow, YouTube 미디어 탭, 종료 라이브 VOD 목록
- 최근 반영 완료: FanPost / ArtistPost / FanLetter base/hot 캐시 분리, 댓글 조건부 짧은 TTL 캐시, FanPost/FanLetter HOT
- 후속 보강 예정: 종료 라이브 replay 자동 publish, 알림

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
- FanPost HOT 후보 조건은 `likeCount >= 5 OR commentCount >= 5`
- FanPost HOT 정렬 점수는 `likeCount + commentCount`
- FanPost HOT은 `scoreCursor + idCursor` 복합커서를 사용한다
- FanLetter HOT 후보 조건은 `likeCount >= 5`
- FanLetter HOT은 `likeCount DESC, id DESC` 뒤 `offset + size`를 사용한다
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
- 이미지 1장만 허용
- 작성 권한은 현재 구독 도메인 조회로 이미 검증한다
- 일반 좋아요는 가능
- 아티스트 special-like는 별도 persisted field로 저장하지 않고 `Reaction` 조회 결과를 DTO에서 조립한다
- special-like는 "수신 멤버 본인" 기준이 아니라 "그 아티스트 소속 멤버 중 한 명이라도 좋아요했는가" 기준이다

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
- `GET /api/member/v1/artists/{artistId}/dashboard`

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
- 아티스트 홈 하이라이트

아티스트 홈 대시보드 응답 구조:

- `latestArtistPost`
- `hotFanPosts`
- `hotFanLetters`

세부 정책:

- `latestArtistPost`: 최신 ArtistPost 1건
- `hotFanPosts`: HOT FanPost 6개
- `hotFanLetters`: HOT FanLetter 4개
- 현재는 대시보드 전체 캐시 없이 조회 시점 조합으로 내려온다

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
- `GET /api/post/v1/artists/{artistId}/fan-posts/hot`
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

FanPost 리스트 `media[]` 정책:

- 리스트에서는 정렬순 앞 `6개`만 내려온다
- `mediaCount`는 전체 첨부 개수다
- 프론트는 `mediaCount - media.length`로 `+N` 오버레이를 계산하면 된다
- 상세에서는 전체 `media[]`가 내려온다

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
- FanPost는 별도 상위 버전 route가 없으므로 현재 route가 곧 최신 구현이다
- 내부적으로는 백엔드가 Redisson 락/atomic update 구조로 바뀌었지만 응답 계약은 그대로다
- 조회 캐시도 이미 1차 반영됐다
- 포스트 본문/작성자/미디어/해시태그/배지 쪽은 base cache
- `likeCount`, `commentCount`는 hot cache로 분리된다
- 댓글 루트 슬라이스/대댓글 목록은 짧은 TTL 조건부 캐시를 쓴다
- HOT 전용 API는 복합커서를 쓴다
  - query param: `scoreCursor`, `idCursor`, `size`
  - 응답: `ApiResponse<ScoreCursorSliceResponse<FanPostResponse>>`
- HOT 결과가 비면 `Hot콘텐츠가 없습니다 더많은 최신글을 확인해 보세요` 문구를 보여주면 된다

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
- 게시글 첨부는 작성/수정 flow 안에서 같이 처리
- 공통 `Media` 엔티티 사용
- `ObjectStorageClient`, `S3ObjectStorageClient`, `MediaService` 구조 있음
- FanPost는 실연동 완료
- ArtistPost용 attach/replace/delete 메서드도 이미 `MediaService`에 준비돼 있음
- YouTube 탭은 게시글 첨부와 별도 모델로 구현 완료

실제 구현 완료 API:

- `POST /api/media/v1/artists/{artistId}/youtube-videos`
- `GET /api/media/v1/artists/{artistId}/youtube-videos`

YouTube 카드 응답 필드:

- `id`
- `artistId`
- `writerMemberId`
- `writerDisplayName`
- `writerProfileImageUrl`
- `youtubeVideoId`
- `youtubeUrl`
- `title`
- `thumbnailUrl`
- `durationSeconds`
- `publishedAt`
- `createdAt`

정책:

- 등록은 아티스트 계정이면서 해당 artist 소속 `ArtistMember`만 가능
- 요청 body는 `youtubeUrl`, `writerArtistMemberId`
- 서버가 YouTube Data API로 메타데이터를 읽어 저장한다
- 목록은 `CursorSliceResponse` 기반 `id DESC`

중요한 운영 주의:

- 로컬 기본 설정은 `media.storage.enabled: false`
- 즉 백엔드 코드상 업로드 구조는 있지만, 환경 설정이 안 되면 실제 업로드는 `MEDIA_STORAGE_NOT_CONFIGURED`로 실패할 수 있다
- 프론트는 업로드 UI를 먼저 만들 수 있지만, 로컬 실테스트 가능 여부는 스토리지 설정 여부에 달려 있다
- YouTube import는 별도로 `YOUTUBE_DATA_API_KEY` 환경변수가 있어야 실제 호출이 된다

## 3-8. Streaming / Live

현재 코드 기준으로 확인된 상태:

- 실시간 스트리밍 메타데이터 생성/시작/종료 구조가 있다
- 스트리밍 채팅 조회/삭제/뮤트 등 대용량 댓글성 처리 구조가 있다
- 실제 방송 송출은 외부 플랫폼(현재 대화 기준으로는 YouTube) 의존 전제가 강하다
- 종료된 라이브 VOD 목록 API가 추가됐다

실제 구현 완료 API:

- `GET /api/v1/artists/{artistId}/lives/vods`
- `PATCH /api/v1/admin/artists/{artistId}/lives/{liveId}/replay`

VOD 카드 응답 필드:

- `liveId`
- `artistId`
- `hostMemberId`
- `hostDisplayName`
- `hostProfileImageUrl`
- `title`
- `thumbnailUrl`
- `replayUrl`
- `durationSeconds`
- `replayPublishedAt`

중요한 주의:

- public VOD 목록에는 `REPLAY_READY` 상태만 노출된다
- 지금은 "방송 종료 -> replay URL 준비 -> replay publish API 호출" 흐름이다
- 즉 완전 자동 업로드 파이프라인은 아직 없고, replay URL 등록이 먼저 되어야 프론트 목록에 보인다

## 4. 부분 구현 상태

## 4-1. ArtistPost

현재 실제 코드 상태:

- 실제 구현 완료 API:
- `POST /api/post/v1/artists/{artistId}/artist-posts`
- `GET /api/post/v1/artists/{artistId}/artist-posts`
- `GET /api/post/v1/artists/{artistId}/artist-posts/{artistPostId}`
- `PATCH /api/post/v1/artists/{artistId}/artist-posts/{artistPostId}`
- `DELETE /api/post/v1/artists/{artistId}/artist-posts/{artistPostId}`
- `POST /api/post/v1/artists/{artistId}/artist-posts/{artistPostId}/likes/toggle`
- `POST /api/post/v1/artists/{artistId}/artist-posts/{artistPostId}/comments`
- `DELETE /api/post/v1/artists/{artistId}/artist-posts/{artistPostId}/comments/{commentId}`
- `GET /api/post/v1/artists/{artistId}/artist-posts/{artistPostId}/comments/{commentId}/replies`
- 추가 비동기 버전 API도 생김:
- `POST /api/post/v3/artists/{artistId}/artist-posts/{artistPostId}/likes/toggle`
- `POST /api/post/v2/artists/{artistId}/artist-posts/{artistPostId}/comments`
- `DELETE /api/post/v2/artists/{artistId}/artist-posts/{artistPostId}/comments/{commentId}`

즉 현재 해석은 아래가 맞다.

- ArtistPost는 FanPost와 거의 같은 응답 구조로 실연동 가능하다
- 작성자는 `ArtistMember.stageName`이 아니라 `Member.nickname`으로 내려온다
- 작성자 옆 체크 표시용 `artistBadge` boolean이 내려온다
- 구독 배지 필드는 없다
- 작성/수정은 `multipart/form-data`
- 권한은 아티스트 계정이면서 해당 artist 소속 artist member인 경우만 허용된다
- ArtistPost 리스트의 `media[]`도 FanPost와 동일하게 앞 `6개` preview만 내려오고, 전체 개수는 `mediaCount`로 본다
- ArtistPost 상세에서는 전체 `media[]`가 내려온다
- 버전 선택 규칙은 고정:
  - `v3`가 있으면 `v3`
  - `v3`는 없고 `v2`까지만 있으면 `v2`
- 따라서 ArtistPost 기준 최신 사용 버전은:
  - 좋아요 = `v3`
  - 댓글 생성/삭제 = `v2`
- `v1` 동기 API는 형태만 남아 있고 실제 사용 경로가 아니다
- 새 `v2/v3`는 비동기 command enqueue 용도라 `202 Accepted` + queued metadata를 반환한다
- ArtistPost 댓글 생성/삭제는 반드시 `v2`를 기준으로 붙여야 한다
- ArtistPost 댓글 `v1`은 레거시 호환용으로만 남아 있다
- 조회 캐시도 이미 반영됐다
- 포스트 본문/작성자/미디어/해시태그/아티스트 배지 쪽은 base cache
- `likeCount`, `commentCount`는 `3초` hot cache를 쓴다
- 댓글 루트 슬라이스/대댓글 목록은 짧은 TTL 조건부 캐시를 쓴다
- 따라서 프론트는 좋아요/댓글 수를 장시간 프론트 단독 truth로 잡기보다 서버 재조회 값으로 다시 맞춰지는 전제를 두는 편이 안전하다

프론트가 지금 해두면 좋은 것:

- ArtistPost 리스트 실연동
- ArtistPost 상세 실연동
- ArtistPost 작성/수정 실연동
- ArtistPost 카드 컴포넌트
- 아티스트 인증 뱃지 UI
- 좋아요 토글 실연동

## 4-2. FanLetter

현재 실제 코드 상태:

- 실API 구현 완료
- `FanLetterController`, `FanLetterService`, `FanLetterRepository` 모두 연결됨
- 경로는 `/api/post/v1/artists/{artistId}/fan-letters/**`
- HOT 경로는 `GET /api/post/v1/artists/{artistId}/fan-letters/hot`
- FanLetter 작성/수정은 `multipart/form-data`
- 수신 대상 선택 가능
  - `recipientType=ARTIST`
  - `recipientType=ARTIST_MEMBER`
  - `recipientArtistMemberId`
- 이미지 1장만 허용
- 댓글 없음
- 좋아요 토글 있음
- special-like 응답 있음
- 작성 권한은 fan membership 기준으로 서버에서 실제 검증함

즉 현재 해석은 아래가 맞다.

- FanLetter는 이제 mock 우선이 아니라 실연동 가능 영역이다
- 다만 목록 응답과 상세 응답의 필드 구성이 다르다
- special-like는 "아티스트 멤버 중 한 명이라도 좋아요했는가" 기준이다
- 조회 캐시도 이미 반영됐다
- 본문/수신자/이미지/special-like 표시용 안정 필드는 base cache
- `likeCount`는 hot cache로 분리돼 있다
- HOT 전용 API는 offset slice를 쓴다
  - query param: `offset`, `size`
  - 응답: `ApiResponse<OffsetSliceResponse<FanLetterHotResponse>>`
- HOT 결과가 비면 `Hot콘텐츠가 없습니다 더많은 최신글을 확인해 보세요` 문구를 보여주면 된다

프론트가 지금 해두면 좋은 것:

- 이미지 중심 카드형 리스트
- FanLetter 상세 모달 또는 상세 페이지
- FanLetter 작성 모달
- 댓글 UI 제거
- special-like 배지 자리 확보
- `To.세븐틴` / `To.민규` 선택 모달

## 4-3. Follow

현재 실제 코드 상태:

- `Follow` 엔티티 / repository / service / controller 구현 완료
- follow 대상은 `ArtistMember`로 고정
- 토글 API 있음
  - `POST /api/member/v1/follows/artist-members/{artistMemberId}/toggle`

즉 현재 해석은 아래가 맞다.

- 일반 SNS형 member-to-member follow가 아니다
- `ArtistMember` 카드/프로필에서만 follow 버튼을 두는 것이 맞다
- 응답은 아래 두 필드다
  - `artistMemberId`
  - `followed`
- 자기 자신의 `ArtistMember`는 follow 불가다

## 4-4. 구독 배지

현재 실제 코드 상태:

- FanPost 응답에 `fanMembershipSubscribed`, `dmSubscribed` 필드가 이미 있다
- 댓글 응답에도 `fanMembershipSubscribed`, `dmSubscribed` 필드가 추가되었다
- 실제 값은 subscription 도메인 batch 조회 결과를 서비스에서 조립해 내려준다

프론트 해석:

- 배지 UI는 먼저 만들어도 된다
- FanPost / 댓글 영역은 실제 값 기준으로 처리해도 된다
- FanLetter 상세는 작성자 배지/프로필을 실제 값 기준으로 처리해도 된다
- FanLetter 목록은 작성자 배지/프로필이 아니라 이미지/수신자/special-like만 렌더링하면 된다

## 4-5. 메인 홈 대시보드

현재 실제 API가 있다.

- `GET /api/member/v1/home/dashboard`

응답 섹션:

- `popularKeywords`
- `subscribedArtistsLatestPosts`
- `followedArtistMembersLatestPosts`

`subscribedArtistsLatestPosts` 해석:

- 활성 fan membership 기준 아티스트별 최신 ArtistPost 2개
- 각 원소는 아래 구조다
  - `artist`
  - `posts`

`followedArtistMembersLatestPosts` 해석:

- follow한 ArtistMember들이 작성한 최신 ArtistPost 총합 6개
- 각 원소는 아래 구조다
  - `artistMemberId`
  - `artist`
  - `stageName`
  - `profileImageUrl`
  - `post`

프론트 해석:

- 메인 홈 대시보드는 이제 mock 전용이 아니라 실연동 가능 영역이다
- 현재 이 API는 로그인 사용자 전용이다
- 비로그인 메인 홈은 아직 별도 API가 없으므로, 지금은 랜딩 화면 + 검색/인기검색어 중심으로 잡는 것이 안전하다
- 검색 입력 자체는 기존 search API를 그대로 쓰고, 대시보드는 홈 진입 시 한 번 더 호출해 섹션을 채우면 된다
- 공식글 카드(`ArtistPostResponse`)는 기존 ArtistPost 카드 컴포넌트를 재사용할 수 있다

## 5. 앞으로 내가 구현할 것

아래는 현재 고정된 우선순위다.

- `5-1`은 이미 1차 반영이 끝난 상태 정리용이다
- `5-2`가 현재 실제 1순위다
- `5-3 ~ 5-5`는 구현은 붙었거나 후순위인 항목들이라, 지금은 실연동/운영보강 우선으로 읽는 것이 맞다

## 5-1. 상태 반영: 포스트페이지 동시성 / 캐싱

백엔드 현재 반영 상태:

- FanPost / ArtistPost / FanLetter는 post base cache + hot count cache로 분리되었다
- `likeCount`, `commentCount` 같은 빠르게 바뀌는 숫자는 hot cache로 읽는다
- ArtistPost hot count는 현재 `3초` TTL 기준으로 맞춰져 있다
- 댓글은 base/hot 분리가 아니라 짧은 TTL 통캐시다
- 부모댓글(root slice)은 보수적으로 조건부 캐시한다
  - `post.commentCount >= 20 AND 10초 안에 5회 조회`
- 자식댓글(replies)은 더 공격적으로 조건부 캐시한다
  - `replyCount >= 20 OR 10초 안에 5회 조회`
- 댓글 캐시 TTL은 현재 `3초`다

프론트 준비 포인트:

- count를 프론트 단독 truth로 오래 들고 가지 않는 구조가 좋다
- optimistic UI는 가능하지만 짧은 시간 뒤 서버 재조회 값으로 덮어쓸 수 있게 짜는 편이 안전하다
- 댓글은 상세 진입 직후와 대댓글 펼침 시 재조회될 수 있으니, 로컬 상태와 서버 상태를 쉽게 동기화할 수 있게 두는 것이 좋다
- HOT/최신 탭 전환이 생겨도 데이터 소스가 분리될 수 있게 컴포넌트를 짜두는 편이 좋다

## 5-2. 필수 1순위: 방금 붙은 기능 프론트 실연동

백엔드 상태:

- 메인 홈 대시보드 / Follow / YouTube 미디어 / VOD 목록까지 1차 구현 완료
- 아직 실환경 검증과 자동화 보강은 덜 끝났다
- 비로그인 메인 홈 전용 guest dashboard API는 아직 없다

프론트 준비 포인트:

- 메인 홈은 `popularKeywords + 구독 아티스트 최신글 + 팔로우 멤버 최신글` 3섹션 기준으로 붙이면 된다
- 비로그인 상태에선 개인화 섹션 대신 랜딩/검색 유도 UI로 분기하는 편이 맞다
- ArtistMember follow 버튼은 실제 토글 API로 연결 가능하다
- 미디어 탭은 YouTube 카드 리스트로 바로 붙일 수 있다
- 라이브 탭은 진행 중 라이브와 별도로 VOD 목록 섹션을 붙일 수 있다

## 5-3. 후순위 기능: YouTube 미디어 탭

백엔드 현재 상태:

- 구현 완료
- 등록은 아티스트 관리성 UI에서 쓰고, 목록은 공개 탭 UI에 붙이면 된다

현재 주의:

- 실호출에는 `YOUTUBE_DATA_API_KEY`가 필요하다
- UI는 `썸네일 + 제목 + 길이 + 업로드일 + 외부 링크` 기준으로 잡는 것이 맞다

## 5-4. 후순위 기능: 종료된 스트리밍 영상 조회

백엔드 현재 상태:

- 구현 완료
- 공개 목록은 있고, publish는 관리자/아티스트 계정이 replay URL을 넣어 주는 방식이다

현재 주의:

- 완전 자동 업로드가 아니라 replay publish 선행이 필요하다
- 프론트는 라이브 목록과 VOD 목록을 분리된 섹션으로 두는 편이 안전하다

## 5-5. 후순위 기능: Follow / 알림

백엔드 예정 가능 작업:

- 알림
- notification feature

현재 해석:

- Follow는 구현 완료다
- 남은 후순위 기능은 사실상 알림 쪽이다

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

## 6-3A. ScoreCursorSliceResponse

FanPost HOT은 아래 응답을 사용한다.

```json
{
  "success": true,
  "data": {
    "content": [],
    "nextScoreCursor": 14,
    "nextIdCursor": 120,
    "hasNext": true,
    "size": 10
  },
  "error": null
}
```

## 6-3B. OffsetSliceResponse

FanLetter HOT은 아래 응답을 사용한다.

```json
{
  "success": true,
  "data": {
    "content": [],
    "nextOffset": 10,
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
- 메인 홈 대시보드
- FanPost 작성
- FanPost 수정/삭제
- FanPost 좋아요
- FanPost 댓글 작성/삭제
- FanLetter 작성/수정/삭제/좋아요
- ArtistMember follow 토글
- YouTube import

아티스트 권한 필요:

- 아티스트 생성
- ArtistMember 관리
- ArtistPost 작성/수정/삭제
- YouTube import
- 라이브 replay publish

중요:

- ArtistPost는 이제 실제 controller/service가 있으므로 공개 GET 실API로 봐도 된다
- 다만 작성/수정/삭제는 아티스트 소속 멤버 권한이 필요하다
- FanLetter도 실제 controller/service가 있으므로 실API로 본다

## 8. 프론트 화면 우선순위 추천

실API 연동 우선:

1. 메인 홈 검색
2. 메인 홈 대시보드
3. 아티스트 상세 헤더
4. FanPost 리스트
5. FanPost 상세
6. FanPost 작성/수정
7. ArtistPost 리스트
8. ArtistPost 상세
9. ArtistPost 작성/수정
10. Hashtag 자동완성
11. FanLetter 리스트
12. FanLetter 상세
13. FanLetter 작성/수정
14. YouTube 미디어 탭
15. VOD 목록
16. Follow 버튼 / 팔로우 섹션

## 9. 프론트가 특히 조심해야 할 것

- FanPost 작성/수정은 JSON API처럼 만들면 안 되고 `multipart/form-data`여야 한다
- ArtistPost 작성/수정도 `multipart/form-data`여야 한다
- FanPost 수정에서 `files`를 보내면 부분 추가가 아니라 전체 교체다
- ArtistPost 수정에서 `files`를 보내면 부분 추가가 아니라 전체 교체다
- FanLetter 작성/수정도 `multipart/form-data`다
- FanLetter도 이미지 교체는 부분 추가가 아니라 전체 교체다
- 댓글은 depth 3 구조를 만들 필요 없다
- replies는 상세 응답에 전부 들어오는 구조가 아니다
- FanPost/댓글/FanLetter 상세의 subscription badge 필드는 지금 실제 값 기준으로 처리해도 된다
- ArtistPost는 FanPost와 비슷하지만 작성자 표기는 `writerNickname + artistBadge` 기준이다
- FanPost / ArtistPost / FanLetter multipart 검증 실패는 이제 공통 400 에러 포맷으로 내려온다
- FanLetter는 텍스트 피드처럼 만들면 정책과 어긋난다
- FanLetter 목록은 작성자 프로필/배지가 기본 노출 대상이 아니다
- HOT은 단순 인기순이 아니라 최근 24시간 제한이 붙는다
- FanPost HOT과 FanLetter HOT의 페이지네이션 방식이 다르다
- FanPost HOT은 `scoreCursor + idCursor`
- FanLetter HOT은 `offset + size`
- 로컬 환경에서는 media storage가 기본 비활성화라 업로드 테스트가 실패할 수 있다
- YouTube import는 `multipart`가 아니라 JSON body다
- YouTube 목록은 외부 재생이 아니라 `youtubeUrl` 링크 이동형으로 해석하는 편이 안전하다
- VOD 목록은 replay publish가 끝난 것만 내려오므로, 종료 직후 바로 안 보일 수 있다

## 10. 프론트 AI에 넘기기 좋은 설명

아래 문장을 그대로 써도 된다.

> `Connectfin-standalone.html`은 비주얼 레퍼런스로만 참고하고, 실제 기능 범위와 데이터 구조는 `FRONTEND_TEAM_VIBE_HANDOFF.md`를 기준으로 작업해줘. 실연동 가능한 것은 메인 홈 검색/대시보드, 아티스트 상세 헤더, FanPost 리스트/상세/작성/수정/좋아요/댓글, ArtistPost 리스트/상세/작성/수정/좋아요/댓글, FanLetter 리스트/상세/작성/수정/좋아요, ArtistMember follow 버튼, YouTube 미디어 탭, VOD 목록이다. FanPost/ArtistPost/FanLetter는 반드시 multipart 작성/수정, cursor infinite scroll, 댓글 depth 2 정책, FanLetter 댓글 없음 구조를 반영해줘.

## 11. 최종 요약

지금 프론트가 가장 안정적으로 붙일 수 있는 실구현 영역은 `검색 + 아티스트 상세 + FanPost 전체 + ArtistPost 전체 + FanLetter 전체 + Hashtag 추천`이다.

추가로 알아둘 현재 백엔드 상태:

- 포스트 조회는 base cache + hot count cache로 분리돼 있다
- 댓글 조회는 짧은 TTL 조건부 캐시를 사용한다
- 따라서 프론트는 숫자 count와 댓글 목록이 아주 짧은 간격으로 재동기화될 수 있음을 전제로 짜는 편이 안전하다

앞으로 내가 구현할 핵심은 아래다.

실연동 추가 가능:

- 메인 홈 대시보드
- ArtistMember Follow
- YouTube 미디어 탭
- 종료된 스트리밍 VOD 목록

후순위 기능:

- 알림
- replay 자동 publish 같은 운영 보강

즉 프론트는 지금 `FanPost + ArtistPost + FanLetter 실연동` 위에 `메인 홈 대시보드`, `Follow`, `YouTube`, `VOD`를 바로 붙일 수 있다. 이후에는 `동시성/캐싱 대비 구조`와 `운영 보강 대응 UI`를 챙기면 된다.
