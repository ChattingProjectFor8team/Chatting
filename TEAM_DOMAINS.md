# ═══════════════════════════════════════════════
# 👥 팀원 도메인 분석 — TEAM DOMAINS
# ═══════════════════════════════════════════════
#
# ⚠️  이 파일은 git pull 후 팀원 코드 변경사항을
#     분석하여 업데이트하는 파일입니다.
#     정민교 작업 로그는 CLAUDE_MEMORY.md 참고.
#
# 업데이트 기록
# - 2026-04-17 14:00 최초 작성
# - 2026-04-17 14:30 fix/env 머지 반영 (JWT_SECRET_KEY .env 주입), 래플 도메인 구조 확인
# ═══════════════════════════════════════════════

---

## 🔴 임호진 (리더) — member 도메인 (인프라 / 인증·인가)

### 패키지
`domain/member/`

### 구현 현황 (2026-04-17 14:30 기준)
| 파일 | 상태 |
|------|------|
| `Artist` 엔티티 | ✅ 구현됨 — soft delete(@SQLDelete), BaseEntity 상속 |
| `ArtistMember` 엔티티 | ✅ 존재 |
| `JwtTokenProvider` | ✅ 구현됨 — JWT 발급/검증, `@Value("${jwt.secret}")` |
| `JwtAuthenticationFilter` | ✅ 구현됨 |
| `MemberDetailsImpl` | ✅ 구현됨 — DB조회 / 토큰 기반 생성 모두 지원 |
| `MemberService` | ❌ 껍데기 |
| `MemberController` | ❌ 껍데기 |
| `MemberRepositoryCustom/Impl` | ✅ QueryDSL 커스텀 구조 존재 |

### 사용 기술
- Spring Security + JWT (`io.jsonwebtoken`)
- JPA + `@SQLDelete` (soft delete)
- QueryDSL (동적 쿼리)
- `BaseEntity` 공통 상속 구조
- `.env` 파일로 `JWT_SECRET_KEY` 주입 (2026-04-17 머지)

### 나(정민교)와의 관계
- `DmSubscription`, `FanMembership` 저장 시 → `user_id`(Member), `artist_id`(Artist) FK 참조
- **BillingKey**도 `user_id` FK → 임호진 Member 엔티티에 의존
- 인증 완료되면 `SecurityContext`에서 userId 꺼내 쓸 예정 → 내 서비스 전반에 영향
- **주의:** MemberService 미완성 상태 → 회원가입 시 `JellyWallet` 자동 생성(createWallet) 연동 타이밍 확인 필요

---

## 🟠 배강혁 (부리더) — dm / raffle 도메인 (DM / 스트리밍 / 래플)

### 패키지
`domain/dm/`

### 구현 현황 (2026-04-17 14:30 기준)
| 파일 | 상태 |
|------|------|
| `DMController` | ✅ 존재 |
| `DMService` | ❌ 껍데기 |
| `DMRepository` | ✅ 존재 |
| `DMDto` | ✅ 존재 |
| `Raffle` 엔티티 | ✅ 존재 (shell) — `domain/raffle/entity/` |
| `RaffleService` | ❌ 껍데기 |
| `ReservoirSampler` | ✅ 구현됨 — Reservoir Sampling 알고리즘, Redis 기반 분산 응모 |
| `RaffleAuditLogger` | ⏳ PR 리뷰 완료, 머지 대기 중 — Redis Streams XADD 감사 로그 |
| `RaffleAuditConsumer` | ⏳ PR 리뷰 완료, 머지 대기 중 — XREADGROUP Consumer Group |
| 스트리밍(Streaming) | ❌ 미구현 |

### 사용 기술
- Redis Streams (`XADD`, `XREADGROUP`, `ACK`) — 감사 로그
- Reservoir Sampling 알고리즘 (균등 분포 추첨)
- Testcontainers (Redis 통합 테스트)
- WebSocket / STOMP (DM 실시간 채팅, 예정)

### 나(정민교)와의 관계
- `DmSubscription` (내 도메인) → DM 입장 권한 체크 주체
- 배강혁의 DM 메시지 발송 로직에서 **내 DmSubscription 상태(ACTIVE 여부)를 검증**해야 함
- 래플 당첨 시 → **내 DmSubscription 기간 연장** 처리 (raffle → subscription 연동)
- **인터페이스 협의 필요:** `DmSubscriptionRepository.isActive(userId, artistId)` 같은 메서드 노출 여부

---

## 🟡 황도윤 — artistcontent 도메인 (홈 / 아티스트 페이지)

### 패키지
`domain/artistcontent/`

### 구현 현황 (2026-04-17 기준)
| 서브 도메인 | 파일 | 상태 |
|-------------|------|------|
| **comment** | CommentService | ❌ 껍데기 |
| **follow** | FollowService | ❌ 껍데기 |
| **hashtag** | HashtagService | ❌ 껍데기 |
| **interaction** | InteractionService | ❌ 껍데기 |
| **media** | MediaService | ❌ 껍데기 |
| **post/artistpost** | ArtistPostService | ❌ 껍데기 |
| **post/fanletter** | FanLetterService | ❌ 껍데기 |
| **post/fanpost** | FanPostService | ❌ 껍데기 |

> 모두 엔티티·DTO·Repository 구조는 잡혀 있음. QueryDSL Custom/Impl 패턴 적용됨.

### 사용 기술
- JPA + QueryDSL (동적 쿼리, 복합 인덱스 활용)
- `RepositoryCustom / RepositoryImpl` 패턴 (No-offset 페이징 예정)
- S3 (이미지 업로드 예정)

### 나(정민교)와의 관계
- `FanLetter` 작성 권한 → **내 FanMembership(ACTIVE)** 여부 검증 필요
- 황도윤이 FanLetterService 구현 시 내 `FanMembershipRepository.isActive(userId, artistId)` 호출 예정
- **인터페이스 협의 필요:** FanMembership 상태 조회 메서드 제공 여부

---

## 📋 pull 후 업데이트 방법

git pull 받은 후 Claude에게 이렇게 말하면 됩니다:

> "깃풀 받았어, TEAM_DOMAINS.md 업데이트해줘"

그러면 Claude가 변경된 파일을 분석하여 아래 항목을 업데이트합니다:
- 구현 현황 표 (껍데기 → 구현됨)
- 새로 추가된 기술 스택
- 나(정민교)와의 관계 변경사항
- 업데이트 날짜/시각

---

## 🔗 전체 도메인 의존 관계 요약

```
[임호진] member
    └─ Artist, Member 엔티티
          ↓ FK 참조
[정민교] payment (BillingKey, DmSubscription, FanMembership, JellyWallet)
          ↓ 구독 상태 제공
[배강혁] dm (DM 입장 권한 체크)
[황도윤] artistcontent (FanLetter 작성 권한 체크)
```
