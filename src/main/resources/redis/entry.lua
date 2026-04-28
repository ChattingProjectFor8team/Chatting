-- entry.lua
-- 응모 처리: closed 확인 + INCR + Reservoir Sampling 확률 판정 + 조건부 candidate SET
-- 이 스크립트가 실행되는 동안 Redis는 다른 명령을 처리하지 않는다 (원자적).
--
-- KEYS[1] = {raffle:{id}:slot:{index}}:closed
-- KEYS[2] = {raffle:{id}:slot:{index}}:count
-- KEYS[3] = {raffle:{id}:slot:{index}}:candidate
-- ARGV[1] = userId (문자열)
-- ARGV[2] = JVM에서 생성한 0.0~1.0 사이의 난수 (문자열)
--
-- 반환값: {순번(int), candidate갱신여부(0or1)}
--   순번 = -1 이면 슬롯 마감으로 응모 거절

-- 1. 슬롯 마감 여부 확인
local closed = redis.call('GET', KEYS[1])
if closed then
    return {-1, 0}
end

-- 2. 순번 획득 (원자적 증가)
local i = redis.call('INCR', KEYS[2])

-- 3. Reservoir Sampling 확률 판정 (1/i 확률로 교체)
local rand = tonumber(ARGV[2])
if rand < (1 / i) then
    redis.call('SET', KEYS[3], ARGV[1], 'KEEPTTL')
    return {i, 1}
end

return {i, 0}
