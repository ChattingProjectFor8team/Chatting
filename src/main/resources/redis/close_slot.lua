-- close_slot.lua
-- 슬롯 종료: closed 플래그 세우기 + candidate/count 최종 읽기
-- 응모 Lua(entry.lua)와 이 스크립트는 Redis 명령 큐에서 반드시 직렬화된다.
-- 네트워크 역전과 무관하게 정합성이 보장된다.
--
-- KEYS[1] = {raffle:{id}:slot:{index}}:closed
-- KEYS[2] = {raffle:{id}:slot:{index}}:candidate
-- KEYS[3] = {raffle:{id}:slot:{index}}:count
--
-- 반환값: {candidate(string), count(int)}
--   candidate가 없으면 빈 문자열 ''

redis.call('SET', KEYS[1], 1, 'EX', 86400)

local candidate = redis.call('GET', KEYS[2])
local count = redis.call('GET', KEYS[3])

return {candidate or '', tonumber(count) or 0}
