# ═══════════════════════════════════════════════
# 👥 팀원 도메인 분석 — TEAM DOMAINS
# ═══════════════════════════════════════════════
#
# ⚠️  이 파일은 git pull 후 팀원 코드 변경사항을
#     분석하여 업데이트하는 파일입니다.
#     정민교 작업 로그는 CLAUDE_MEMORY.md 참고.
#
# 업데이트 기록: 2026-04-17 최초 작성 (pull 기준)
# ═══════════════════════════════════════════════

---

## 🔴 임호진 (리더) — member 도메인 (인프라 / 인증·인가)

### 패키지
`domain/member/`

### 구현 현황 (2026-04-17 기준)
| 파일 | 상태 |
|------|------|
| `Artist` 엔티티 | ✅ 구현됨 — soft delete(@SQLDelete), BaseEntity 상속 |
| `ArtistMember` 엔티티 | ✅ 존재 |
| `MemberService` | ❌ 껍데기 |
| `MemberController` | ❌ 껍데기 |
| `MemberRepositoryCustom/Impl` | ✅ QueryDSL 커스텀 구조 존재 |

### 사용 기술
- Spring Security (인증/인가 예정)
- JPA + `@SQLDelete` (soft delete)
- QueryDSL (동적 쿼리)
- `BaseEntity` 공통 상속 구조

### 나(정민교)와의 관계
- `DmSubscription`, `FanMembership` 저장 시 → `user_id`(Member), `artist_id`(Artist) FK 참조
- **BillingKey**도 `user_id` FK → 임호진 Member 엔티티에 의존
- 인증 완료되면 `SecurityContext`에서 userId 꺼내 쓸 예정 → 내 서비스 전반에 영향
- **주의:** MemberService 미완성 상태 → 회원가입 시 `JellyWallet` 자동 생성(createWallet) 연동 타이밍 확인 필요

---

## 🟠 배강혁 (부리더) — dm / raffle 도메인 (DM / 스트리밍 / 래플)

### 패키지
`domain/dm/`

### 구현 현황 (2026-04-17 기준)
| 파일 | 상태 |
|------|------|
| `DMController` | ✅ 존재 |
| `DMService` | ❌ 껍데기 |
| `DMRepository` | ✅ 존재 |
| `DMDto` | ✅ 존재 |
| 래플(Raffle) | ❌ 아직 미확인 (별도 패키지 없음) |
| 스트리밍(Streaming) | ❌ 아직 미확인 |

### 사용 기술 (예정)
- WebSocket / STOMP (실시간 채팅)
- Redis Pub/Sub (다중 서버 채팅 전파)
- Redisson 분산락 (DM 구독 동시성 제어)

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
