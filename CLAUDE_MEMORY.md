# Claude Memory — INFINEAT Entertainment Project

> 대화가 끊겼을 때 이 파일을 Claude에게 보여주면 바로 이어서 작업할 수 있습니다.
> 작업 완료 시마다 [완료 로그]에 타임스탬프와 함께 기록합니다.

---

## 프로젝트 개요

- **프로젝트명**: INFINEAT Entertainment (아티스트/팬 소통 플랫폼)
- **팀명**: Team Infinite
- **나 (정민교) 담당**: 결제(Payment) / 젤리(Jelly, 포인트) / 멤버십(Membership)
- **팀원 역할**:
  - 임호진 (리더): 인프라, 인증/인가
  - 배강혁 (부리더): DM, 스트리밍, 래플
  - 황도윤: 홈, 아티스트 페이지

---

## 작업 규칙 (Claude ↔ 정민교)

- **코드 작성**: Claude가 코드를 보여주면 정민교가 직접 타이핑. Edit/Write로 코드 직접 수정 금지.
- **Git 원격 작업**: push, PR 생성은 정민교가 직접. 명령어만 순서대로 안내.
- **커밋 단위**: 기능/파일 단위로 잘게 → 완성 후 멈추고 커밋 여부 확인 → 완료 확인 후 다음 작업.

---

## 팀 컨벤션

- **브랜치**: `Feat/#이슈번호-도메인-기능` (예: `Feat/#12-Auth-Login`)
- **커밋**: `특성: 메시지` — 특성 첫 글자 대문자, 콜론 뒤 한 칸
- **이슈 본문**: `## 💡 기능 상세 내용` / `## ✅ 완료 조건` / `## 🔗 기타 참고 사항`

---

## 완료 로그

> 형식: `날짜 시:분 — 무엇을 / 어떻게 / 어디까지`

### [이전 대화 — 정확한 시각 불명, 2026-04-17 이전]

- **Jelly 도메인 구현 완료**
  - JellyService: 잔액 조회, 이력 조회(페이징), 충전, 사용, 환불, 지갑 생성
  - JellyController: 위 기능 REST API로 노출
  - JellyProperties: 젤리 단가 등 설정값 관리
  - 비관적 락(SELECT FOR UPDATE) 적용하여 동시성 제어
  - `UserJellyBalance`, `JellyTransaction` 엔티티 + Repository 완성

- **Billing 도메인 구현 완료**
  - BillingService: 카드 등록 / 목록 조회 / 삭제 / 대표카드 지정
  - BillingController: 위 기능 REST API로 노출
  - PortOneClient: RestClient 기반 PortOne API 연동, 생성자 주입, 타임아웃 설정
  - 대표카드 변경 시 Redisson 분산락 적용
  - PortOne 호출을 별도 트랜잭션으로 분리 (외부 API 호출 트랜잭션 오염 방지)
  - BillingKeyRequest / BillingKeyResponse DTO 완성
  - `BillingKey` 엔티티 + Repository 완성
  - BILLING_KEY_NOT_FOUND 에러코드 추가

- **엔티티/Repository 생성 완료 (로직 미완)**
  - `DmSubscription`, `FanMembership` — 엔티티 + Repository만 존재
  - `AutoChargeSetting`, `AutoChargeHistory` — 엔티티 + Repository만 존재
  - `SubscriptionMembership` — 별도 패키지에 Controller/Service 껍데기만 존재

---

## TODO (앞으로 할 작업 계획)

> 우선순위 순서대로 작성

### 1순위 — DmSubscription / FanMembership 서비스 구현
- [ ] `SubscriptionMembershipService` 로직 작성
  - DM 구독 구매 (젤리 차감 → dm_subscription 저장)
  - 팬 멤버십 구매 (젤리 차감 → fan_membership 저장)
  - 구독 상태 조회 (현재 ACTIVE 여부)
  - 만료 처리 (status → EXPIRED)
- [ ] `SubscriptionMembershipController` API 연결
  - `POST /api/v1/subscription/dm/{artistId}`
  - `POST /api/v1/subscription/membership/{artistId}`
  - `GET /api/v1/subscription/status/{artistId}`

### 2순위 — AutoCharge 서비스 구현
- [ ] `AutoChargeService` 신규 작성
  - 자동충전 설정 등록/수정/해제
  - 젤리 사용 시 잔액이 threshold 이하이면 자동충전 트리거
  - `AutoChargeHistory`에 성공/실패 이력 기록
- [ ] JellyService의 `use()` 메서드에 AutoCharge 트리거 연동

### 3순위 — Refund (환불) 테이블 분리 처리
- [ ] `Refund` 엔티티 + Repository 생성 (ERD 기준)
- [ ] `RefundService` 작성
  - PortOne 환불 API 호출
  - `refund` 테이블에 환불 이력 저장
  - 젤리 회수 (JellyService.refund() 연동)
- [ ] 환불 가능 조건 검증 로직 (REFUND_NOT_ELIGIBLE 등 에러코드 활용)

---

## 주요 파일 경로

```
src/main/java/com/example/infinite/domain/
├── payment/
│   ├── controller/    BillingController, JellyController
│   ├── service/       BillingService, JellyService
│   ├── entity/        BillingKey, UserJellyBalance, JellyTransaction,
│   │                  AutoChargeSetting, AutoChargeHistory, DmSubscription, FanMembership
│   ├── repository/    (위 엔티티 전부)
│   ├── dto/           BillingKeyRequest/Response, JellyBalance, JellyHistory
│   ├── config/        JellyProperties
│   └── client/        PortOneClient
└── subscriptionmembership/
    ├── controller/    SubscriptionMembershipController  ← 껍데기
    ├── service/       SubscriptionMembershipService     ← 껍데기
    ├── entity/        SubscriptionMembership
    └── repository/    SubscriptionMembershipRepository
```
