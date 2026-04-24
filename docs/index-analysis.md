# 인덱스 설계 분석 및 EXPLAIN 검증

> 강혁 도메인(DM / Raffle / Live)의 쿼리-인덱스 매핑을 분석하고, 누락 인덱스를 추가한 뒤 5만건+ 더미 데이터로 EXPLAIN 결과를 비교 수집했다.

## 1. 검증 환경

| 항목 | 값 |
|------|-----|
| DBMS | MySQL 8.4 (로컬) |
| 데이터 규모 | dm_messages 50,000 / raffle_entries 50,000 / live_chat_messages 50,000 + 마스터 300건 |
| 더미 데이터 삽입 시간 | 2.6초 (`SET autocommit = 0; CALL ...; COMMIT` 으로 트랜잭션 묶음) |

## 2. 추가한 인덱스

### B-1. `idx_dm_room_artist` on `dm_rooms (artist_id)`

- **문제 쿼리**: `DmRoomRepository.findByArtistId(artistId)` — broadcast 호출 시 아티스트의 전체 DM 방을 조회
- **원인**: 기존 `uk_dm_room_user_artist (user_id, artist_id)` 는 `user_id` 접두사 조건이 없으면 활용 불가
- **추가 이유**: InnoDB 가 PK(`id`)를 보조 인덱스 리프에 자동 포함하므로 `(artist_id)` 단독 선언으로 `(artist_id, id)` 효과

### B-2. `idx_raffle_entry_user_entered` on `raffle_entries (user_id, entered_at)`

- **문제 쿼리**: `RaffleEntryRepository.findByUserIdOrderByEnteredAtDesc(userId)` — "내 응모 내역" 조회
- **원인**: 기존 `uk_raffle_entry_user (raffle_id, user_id)` 는 `user_id` 가 두 번째 컬럼이라 단독 조회에 쓸 수 없음 + `entered_at` 정렬도 커버 불가
- **추가 이유**: `(user_id, entered_at)` 복합으로 WHERE 필터 + ORDER BY 정렬을 인덱스 한 번에 처리 → filesort 제거

### B-3. DmMessage / LiveChatMessage 커서 쿼리 — 추가 불필요

실측 EXPLAIN 결과 두 테이블 모두 `ORDER BY id DESC` 쿼리를 **PK Backward index scan** 으로 처리. filesort 없음 → 인덱스 추가 불요. (지시서 판단 기준: `type: ref` + `Extra`에 `filesort` 없으면 기존 인덱스 충분)

## 3. EXPLAIN 결과 — Before / After 비교

```
══════════════════════════════════════
 EXPLAIN 결과 ①: DmRoom.findByArtistId
──────────────────────────────────────
 [Before] type=ALL,  key=NULL,                rows=100,   Extra=Using where
 [After]  type=ref,  key=idx_dm_room_artist,  rows=20,    Extra=(none)
══════════════════════════════════════

══════════════════════════════════════
 EXPLAIN 결과 ②: RaffleEntry.findByUserIdOrderByEnteredAtDesc
──────────────────────────────────────
 [Before] type=ALL,  key=NULL,                           rows=49,504, Extra=Using where; Using filesort
 [After]  type=ref,  key=idx_raffle_entry_user_entered,  rows=50,     Extra=Backward index scan
══════════════════════════════════════

══════════════════════════════════════
 EXPLAIN 결과 ③: DmMessage cursor (dm_room_id=1 AND id<400 ORDER BY id DESC)
──────────────────────────────────────
 type=range, key=PRIMARY, rows=399, Extra=Using where; Backward index scan
  → 변화 없음 (PK 사용으로 filesort 없음 → 인덱스 추가 불필요)
══════════════════════════════════════

══════════════════════════════════════
 EXPLAIN 결과 ④: LiveChatMessage cursor (live_stream_id=1 AND id<400 ORDER BY id DESC)
──────────────────────────────────────
 type=range, key=PRIMARY, rows=399, Extra=Using where; Backward index scan
  → 변화 없음 (동일 이유로 인덱스 추가 불필요)
══════════════════════════════════════

══════════════════════════════════════
 EXPLAIN 결과 ⑤: RealtimeLive VOD cursor (artist_id=1 AND live_status='ENDED' AND id<50 ORDER BY id DESC)
──────────────────────────────────────
 type=ref, key=idx_live_artist_created, rows=20,
 Extra=Using index condition; Using where; Using filesort
  → 옵티마이저가 (artist_id, created_at) 인덱스를 선택 → id DESC 정렬에 filesort 발생
  → 본 Phase 범위 외. 현재 빈도·비용을 고려하면 허용 가능 (데이터 소규모, 저빈도 조회)
══════════════════════════════════════
```

### 핵심 수치

| 쿼리 | Before rows | After rows | 개선 |
|------|-------------|------------|------|
| DmRoom.findByArtistId | 100 (풀 스캔) | 20 | 5× (100% → 20%) |
| RaffleEntry.findByUser… | 49,504 (풀 스캔 + filesort) | 50 | **990×** + filesort 제거 |

## 4. 엔티티 영속화

수동 `CREATE INDEX`는 EXPLAIN 검증용이며, 엔티티 `@Index` 선언으로 `ddl-auto: create-drop/update` 가 자동 생성하도록 반영했다.

- `DmRoom.java`: `idx_dm_room_artist` 추가
- `RaffleEntry.java`: `idx_raffle_entry_user_entered` 추가
