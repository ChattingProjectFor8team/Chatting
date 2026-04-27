# API Overview

이 문서는 README에서 링크되는 **도메인별 API 빠른 참조 문서**다.

- Base URL: `/`
- 주요 Prefix:
  - `auth`: `/api/auth/v1`
  - `member`: `/api/member`, `/api/member/admin`
  - `post`: `/api/post`
  - `media`: `/api/media/v1`
  - `realtime / raffle / subscription`: `/api/v1`
  - `payment`: `/api/payment/v1`, `/api/v1/auto-charge`
- 인증:
  - 보호된 API는 기본적으로 `Authorization: Bearer {accessToken}`
  - 일부 결제 / 자동충전 레거시 API는 현재 `X-User-Id` 헤더를 사용한다
- WebSocket / STOMP:
  - handshake endpoint: `/ws-stomp`
  - publish prefix: `/pub`
  - subscribe prefix: `/sub`

## 공통 응답 규칙

### ApiResponse

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

### PageResponse

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

### CursorSliceResponse

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

### ScoreCursorSliceResponse

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

### OffsetSliceResponse

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

## 1. Auth / Member / Artist

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/auth/v1/signup` | 회원가입 |
| `POST` | `/api/auth/v1/login` | 로그인 / 토큰 발급 |
| `GET` | `/api/member/v1/me` | 내 정보 조회 |
| `PATCH` | `/api/member/v1/me` | 내 정보 수정 (`application/json` 또는 `multipart/form-data`) |
| `PATCH` | `/api/member/v1/me/password` | 비밀번호 변경 |
| `PATCH` | `/api/member/v1/me/email` | 이메일 변경 + 새 토큰 발급 |
| `PATCH` | `/api/member/admin/v1/members/{memberId}/role` | 관리자용 역할 변경 |
| `PATCH` | `/api/member/admin/v1/members/{memberId}/status` | 관리자용 상태 변경 |
| `GET` | `/api/member/v1/artists/search` | 아티스트 검색 원본 조회 |
| `GET` | `/api/member/v2/artists/search` | 아티스트 검색 로컬 캐시 버전 |
| `GET` | `/api/member/v3/artists/search` | 아티스트 검색 Redis 캐시 기본 버전 |
| `GET` | `/api/member/v1/artists/search/popular` | 인기 검색어 조회 |
| `GET` | `/api/member/v1/artists/{artistId}` | 아티스트 상세 원본 조회 |
| `GET` | `/api/member/v2/artists/{artistId}` | 아티스트 상세 Redis cache-aside 조회 |
| `POST` | `/api/member/v1/artists` | 아티스트 생성 (`application/json` 또는 `multipart/form-data`) |
| `PATCH` | `/api/member/v1/artists/{artistId}` | 아티스트 수정 (`application/json` 또는 `multipart/form-data`) |
| `DELETE` | `/api/member/v1/artists/{artistId}` | 아티스트 삭제 |
| `POST` | `/api/member/v1/artists/{artistId}/members` | 아티스트 멤버 생성 (`application/json` 또는 `multipart/form-data`) |
| `PATCH` | `/api/member/v1/artists/{artistId}/members/{artistMemberId}` | 아티스트 멤버 수정 (`application/json` 또는 `multipart/form-data`) |
| `DELETE` | `/api/member/v1/artists/{artistId}/members/{artistMemberId}` | 아티스트 멤버 삭제 |

## 2. Home / Follow

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/api/member/v1/home/dashboard` | 메인 홈 대시보드 |
| `GET` | `/api/member/v1/artists/{artistId}/dashboard` | 아티스트 홈 대시보드 |
| `POST` | `/api/member/v1/follows/artist-members/{artistMemberId}/toggle` | ArtistMember 팔로우 토글 |

## 3. FanPost

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/post/v1/artists/{artistId}/fan-posts` | FanPost 작성 |
| `GET` | `/api/post/v1/artists/{artistId}/fan-posts` | FanPost 목록 |
| `GET` | `/api/post/v1/artists/{artistId}/fan-posts/hot` | FanPost HOT 목록 |
| `GET` | `/api/post/v1/artists/{artistId}/fan-posts/{fanPostId}` | FanPost 상세 |
| `PATCH` | `/api/post/v1/artists/{artistId}/fan-posts/{fanPostId}` | FanPost 수정 |
| `DELETE` | `/api/post/v1/artists/{artistId}/fan-posts/{fanPostId}` | FanPost 삭제 |
| `POST` | `/api/post/v1/artists/{artistId}/fan-posts/{fanPostId}/likes/toggle` | FanPost 좋아요 토글 |
| `POST` | `/api/post/v1/artists/{artistId}/fan-posts/{fanPostId}/comments` | FanPost 댓글 작성 |
| `POST` | `/api/post/v1/artists/{artistId}/fan-posts/{fanPostId}/comments/{commentId}/likes/toggle` | FanPost 댓글 좋아요 토글 |
| `DELETE` | `/api/post/v1/artists/{artistId}/fan-posts/{fanPostId}/comments/{commentId}` | FanPost 댓글 삭제 |
| `GET` | `/api/post/v1/artists/{artistId}/fan-posts/{fanPostId}/comments/{commentId}/replies` | FanPost 대댓글 조회 |

## 4. ArtistPost

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/post/v1/artists/{artistId}/artist-posts` | ArtistPost 작성 |
| `GET` | `/api/post/v1/artists/{artistId}/artist-posts` | ArtistPost 목록 |
| `GET` | `/api/post/v1/artists/{artistId}/artist-posts/{artistPostId}` | ArtistPost 상세 |
| `PATCH` | `/api/post/v1/artists/{artistId}/artist-posts/{artistPostId}` | ArtistPost 수정 |
| `DELETE` | `/api/post/v1/artists/{artistId}/artist-posts/{artistPostId}` | ArtistPost 삭제 |
| `POST` | `/api/post/v1/artists/{artistId}/artist-posts/{artistPostId}/likes/toggle` | ArtistPost 좋아요 동기 토글 |
| `POST` | `/api/post/v3/artists/{artistId}/artist-posts/{artistPostId}/likes/toggle` | ArtistPost 좋아요 비동기 enqueue 토글 (`202 Accepted`) |
| `POST` | `/api/post/v1/artists/{artistId}/artist-posts/{artistPostId}/comments` | ArtistPost 댓글 동기 생성 |
| `DELETE` | `/api/post/v1/artists/{artistId}/artist-posts/{artistPostId}/comments/{commentId}` | ArtistPost 댓글 동기 삭제 |
| `POST` | `/api/post/v2/artists/{artistId}/artist-posts/{artistPostId}/comments` | ArtistPost 댓글 비동기 생성 |
| `DELETE` | `/api/post/v2/artists/{artistId}/artist-posts/{artistPostId}/comments/{commentId}` | ArtistPost 댓글 비동기 삭제 |
| `POST` | `/api/post/v1/artists/{artistId}/artist-posts/{artistPostId}/comments/{commentId}/likes/toggle` | ArtistPost 댓글 좋아요 토글 |
| `GET` | `/api/post/v1/artists/{artistId}/artist-posts/{artistPostId}/comments/{commentId}/replies` | ArtistPost 대댓글 조회 |

## 5. FanLetter

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/post/v1/artists/{artistId}/fan-letters` | FanLetter 작성 |
| `GET` | `/api/post/v1/artists/{artistId}/fan-letters` | FanLetter 목록 |
| `GET` | `/api/post/v1/artists/{artistId}/fan-letters/hot` | FanLetter HOT 목록 |
| `GET` | `/api/post/v1/artists/{artistId}/fan-letters/{fanLetterId}` | FanLetter 상세 |
| `PATCH` | `/api/post/v1/artists/{artistId}/fan-letters/{fanLetterId}` | FanLetter 수정 |
| `DELETE` | `/api/post/v1/artists/{artistId}/fan-letters/{fanLetterId}` | FanLetter 삭제 |
| `POST` | `/api/post/v1/artists/{artistId}/fan-letters/{fanLetterId}/likes/toggle` | FanLetter 좋아요 토글 |

> FanLetter는 현재 댓글 API가 없다.

## 6. Hashtag / Media / VOD

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/api/post/v1/hashtags/suggestions` | 해시태그 추천 |
| `POST` | `/api/media/v1/artists/{artistId}/youtube-videos` | YouTube 링크 import 후 메타데이터 카드 저장 |
| `GET` | `/api/media/v1/artists/{artistId}/youtube-videos` | YouTube 카드 목록 (`id DESC` cursor) |
| `GET` | `/api/v1/artists/{artistId}/lives/vods` | 종료 라이브 VOD 목록 |
| `PATCH` | `/api/v1/admin/artists/{artistId}/lives/{liveId}/replay` | VOD replay publish |

## 7. DM / Live

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/dm/rooms` | 내 DM 방 목록 |
| `GET` | `/api/v1/dm/rooms/{roomId}/messages` | DM 메시지 목록 (`before` cursor) |
| `GET` | `/api/v1/admin/artists/{artistId}/dm/rooms` | 관리자용 아티스트 DM 방 목록 |
| `GET` | `/api/v1/artists/{artistId}/lives` | 라이브 목록 |
| `GET` | `/api/v1/artists/{artistId}/lives/{liveId}` | 라이브 상세 |
| `GET` | `/api/v1/artists/{artistId}/lives/{liveId}/chat/messages` | 라이브 채팅 내역 (`before` cursor) |
| `POST` | `/api/v1/admin/artists/{artistId}/lives` | 관리자용 라이브 생성 |
| `PATCH` | `/api/v1/admin/artists/{artistId}/lives/{liveId}/start` | 라이브 시작 |
| `PATCH` | `/api/v1/admin/artists/{artistId}/lives/{liveId}/end` | 라이브 종료 |
| `DELETE` | `/api/v1/admin/artists/{artistId}/lives/{liveId}/chat/messages/{messageId}` | 라이브 채팅 메시지 삭제 |
| `POST` | `/api/v1/admin/artists/{artistId}/lives/{liveId}/chat/mute/{userId}` | 라이브 채팅 유저 mute |
| `DELETE` | `/api/v1/admin/artists/{artistId}/lives/{liveId}/chat/mute/{userId}` | 라이브 채팅 유저 unmute |
| `WebSocket/STOMP` | `/pub/dm/{roomId}` | DM 개별 룸 메시지 발행 |
| `WebSocket/STOMP` | `/pub/dm/broadcast/{artistId}` | 아티스트 브로드캐스트 DM 발행 |
| `WebSocket/STOMP` | `/sub/dm/{roomId}` | DM 개별 룸 메시지 구독 |
| `WebSocket/STOMP` | `/pub/live/{liveId}/chat` | 라이브 채팅 발행 |
| `WebSocket/STOMP` | `/sub/live/{liveId}` | 라이브 채팅 구독 |
| `WebSocket/STOMP` | `/sub/user/{userId}/notifications` | 개인 알림 구독 |

## 8. Payment / Subscription / Membership

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/payment/v1/payments/prepare` | 결제 준비 / paymentId 발급 |
| `POST` | `/api/payment/v1/payments/webhook` | PortOne 웹훅 수신 |
| `GET` | `/api/payment/v1/jelly/balance` | 젤리 잔액 조회 |
| `GET` | `/api/payment/v1/jelly/histories` | 젤리 거래 이력 조회 |
| `POST` | `/api/payment/v1/billings` | 빌링키 등록 |
| `GET` | `/api/payment/v1/billings` | 빌링키 목록 조회 |
| `DELETE` | `/api/payment/v1/billings/{billingId}` | 빌링키 삭제 |
| `POST` | `/api/v1/auto-charge/setting` | 자동충전 설정 등록 / 수정 |
| `GET` | `/api/v1/auto-charge/setting` | 자동충전 설정 조회 |
| `DELETE` | `/api/v1/auto-charge/setting` | 자동충전 비활성화 |
| `GET` | `/api/v1/auto-charge/histories` | 자동충전 실행 이력 조회 |
| `POST` | `/api/payment/v1/refunds/payments/{paymentId}` | 수동결제 환불 |
| `POST` | `/api/payment/v1/refunds/auto-charges/{historyId}` | 자동충전 환불 |
| `POST` | `/api/payment/v1/refunds/dm-subscriptions/{subscriptionId}` | DM 구독권 환불 |
| `POST` | `/api/v1/subscriptions/dm/{artistId}` | DM 구독 구매 |
| `POST` | `/api/v1/subscriptions/membership/{artistId}` | 팬 멤버십 구매 |
| `GET` | `/api/v1/subscriptions/dm/{artistId}/status` | DM 구독 상태 조회 |
| `GET` | `/api/v1/subscriptions/membership/{artistId}/status` | 팬 멤버십 상태 조회 |
| `GET` | `/api/v1/subscriptions/dm/history` | DM 구독 이력 조회 |
| `GET` | `/api/v1/subscriptions/membership/history` | 팬 멤버십 이력 조회 |

## 9. Raffle

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/artists/{artistId}/raffles/{raffleId}/entries` | 래플 응모 |
| `GET` | `/api/v1/artists/{artistId}/raffles` | 래플 목록 |
| `GET` | `/api/v1/artists/{artistId}/raffles/{raffleId}` | 래플 상세 |
| `GET` | `/api/v1/artists/{artistId}/raffles/{raffleId}/entries/me` | 내 응모 결과 조회 |
| `GET` | `/api/v1/users/me/raffle-entries` | 내 전체 응모 이력 조회 |
| `POST` | `/api/v1/admin/artists/{artistId}/raffles` | 관리자용 래플 생성 |
| `PATCH` | `/api/v1/admin/artists/{artistId}/raffles/{raffleId}/start` | 래플 시작 |
| `PATCH` | `/api/v1/admin/artists/{artistId}/raffles/{raffleId}/cancel` | 래플 취소 |
| `GET` | `/api/v1/admin/artists/{artistId}/raffles/{raffleId}/slots` | 슬롯 상태 조회 |
| `PATCH` | `/api/v1/admin/artists/{artistId}/raffles/{raffleId}/winners/{winnerId}/reward-status` | 당첨 보상 상태 변경 |

## 참고

- 캐시 / 동시성 / 인덱스 분석은 README가 아니라 개별 문서로 분리해 두었다.
- 실사용 세부 계약은 아래 문서를 함께 보면 된다.
  - [`artist-search-caching.md`](artist-search-caching.md)
  - [`artistcontent-concurrency-comparison.md`](artistcontent-concurrency-comparison.md)
  - [`member-artistcontent-index-analysis.md`](member-artistcontent-index-analysis.md)
  - [`../FRONTEND_TEAM_VIBE_HANDOFF.md`](../FRONTEND_TEAM_VIBE_HANDOFF.md)
