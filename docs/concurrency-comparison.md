# 동시성 제어 3종 비교 — 젤리 동시 차감 시나리오

## 1. 문제 정의

여러 스레드(또는 요청)가 동시에 같은 사용자의 젤리 잔액을 차감하면,
각 스레드가 읽은 시점의 잔액이 동일하여 서로의 차감 결과를 덮어쓴다.
이를 **Lost Update**(갱신 손실)라고 하며, 데이터 정합성이 깨진다.

## 2. 테스트 설계

### 시나리오

| 항목 | 값 |
|------|-----|
| 초기 잔액 | 10,000 젤리 |
| 동시 차감 | 100개 스레드, 각 100 젤리 |
| 기대 결과 | 100 × 100 = 10,000 차감 → 잔액 0 |
| 동기화 | `CyclicBarrier(100)` — 모든 스레드가 준비된 후 동시 출발 |

### 환경

- Java 21, Spring Boot 4.0.0, MySQL (로컬)
- HikariCP: maximum-pool-size=110, connection-timeout=120000ms
- JUnit 5 + `@SpringBootTest` + `@ActiveProfiles("test")`

## 3. 테스트 결과

| 전략 | 성공 | 실패 | 최종 잔액 | 정합성 | 비고 |
|------|------|------|-----------|--------|------|
| 락 없음 (read-modify-write) | 100 | 0 | 9,900 (> 0) | ❌ | Lost Update 약 99건 |
| 비관적 락 (`FOR UPDATE`) | 100 | 0 | 0 | ✅ | 순차 처리 |
| 낙관적 락 (`@Version`) | 1 | 99 | 9,900 | ✅ | 버전 충돌로 일부 실패 |

> 위 수치는 `./gradlew test --tests "*.JellyConcurrencyTest" -i` 실행 결과이며,
> 락 없음 테스트의 Lost Update 건수와 낙관적 락의 성공/실패 비율은 실행마다 달라질 수 있다.
> (`CyclicBarrier` 로 100 스레드를 동시 출발시키는 극단 시나리오에서는 낙관적 락 성공이 1건에 수렴하는 경향.)

## 4. 전략별 분석

### 락 없음 — 왜 깨지는가

```
Thread A: SELECT balance → 10000
Thread B: SELECT balance → 10000  (A와 같은 값을 읽음)
Thread A: UPDATE balance = 9900   (10000 - 100)
Thread B: UPDATE balance = 9900   (10000 - 100, A의 차감을 덮어씀)
→ 200 차감했지만 100만 반영됨
```

### 비관적 락 (`SELECT ... FOR UPDATE`)

- **원리**: 행을 읽는 시점에 배타적 잠금(X-Lock)을 획득한다. 다른 트랜잭션은 잠금 해제까지 대기.
- **장점**: 모든 요청이 순차 처리되어 100% 성공, 데이터 정합성 보장
- **단점**: 대기 시간 발생 → 처리량(throughput) 저하. 커넥션 풀 크기 주의 필요.
- **적합한 경우**: 충돌 빈도가 높거나, 실패 재시도 로직이 복잡한 경우 (예: 결제)

```java
// Repository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM UserJellyBalance b WHERE b.userId = :userId")
Optional<UserJellyBalance> findByUserIdForUpdate(@Param("userId") Long userId);

// Service — 비관적 락으로 조회 후 차감
UserJellyBalance wallet = findWalletForUpdate(userId);
wallet.use(amount);
```

### 낙관적 락 (`@Version`)

- **원리**: 읽을 때 잠금 없음. 쓸 때 version 값이 바뀌었으면 `ObjectOptimisticLockingFailureException` 발생.
- **장점**: 읽기 시 잠금이 없어 동시 읽기 처리량이 높음
- **단점**: 충돌 시 실패 → 재시도 로직 필요. 충돌이 잦으면 재시도 비용이 비관적 락 대기보다 큼.
- **적합한 경우**: 충돌이 드물고, 실패 시 재시도가 간단한 경우 (예: 좋아요)

```java
// Entity
@Version
private Integer version;

// 충돌 시 UPDATE 영향 행 수 = 0 → 예외 발생
// UPDATE user_jelly_wallet SET current_balance=?, version=? WHERE user_id=? AND version=?
```

## 5. 우리 프로젝트의 선택: 비관적 락

젤리 차감은 **결제 도메인**이므로:

- 차감 실패 = 사용자에게 재시도 요청 = UX 저하
- 충돌 빈도가 높음 (구독 결제 시점에 동시 요청 가능)
- 자동충전 트리거 등 후속 로직이 있어 재시도 시 부수효과 관리가 복잡

따라서 "대기하더라도 전부 성공"하는 비관적 락이 적합하다.

> **참고**: `@Version` 필드는 낙관적 락 비교 테스트를 위해 추가했으나,
> 운영 코드(`JellyService.use()`)는 `findByUserIdForUpdate()`(비관적 락)를 사용하므로
> `@Version` 충돌이 발생하지 않는다. FOR UPDATE가 행 잠금을 먼저 잡기 때문.

## 6. 실행 방법

### 사전 조건

1. **MySQL** 로컬 실행 (port 3306)
   ```sql
   -- 테스트 DB 생성 (최초 1회)
   CREATE DATABASE IF NOT EXISTS infinite_test;
   -- 비밀번호 확인: application-test.yml의 spring.datasource.password와 일치해야 함
   ```
2. **Redis** 로컬 실행 (port 6379)
   ```bash
   docker run -d --name test-redis -p 6379:6379 redis:7-alpine
   ```
3. **MySQL max_connections 확인** (비관적 락 테스트에서 110개 커넥션 사용)
   ```sql
   SHOW VARIABLES LIKE 'max_connections';
   -- 151 이상이어야 함 (MySQL 기본값 151이므로 보통 문제없음)
   ```

### 테스트 실행

```bash
./gradlew test --tests "*.JellyConcurrencyTest" -i
```

`-i` (info 레벨)을 붙여야 `System.out.println` 출력이 콘솔에 보인다.
