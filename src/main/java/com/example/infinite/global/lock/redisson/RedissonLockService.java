package com.example.infinite.global.lock.redisson;

import com.example.infinite.global.lock.LockException;
import com.example.infinite.global.lock.LockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component("redissonLockService")
@RequiredArgsConstructor
public class RedissonLockService implements LockService {

    /**
     * Redisson이 제공하는 RLock 추상화를 감싼 분산 락 서비스다.
     *
     * 학습 포인트:
     * - V1처럼 SET NX를 직접 다루지 않아도 되고
     * - tryLock / unlock / 스레드 소유권 확인 같은 공통 기능을 라이브러리가 맡아준다.
     *
     * 그래서 V2 이후는 "락을 어디에 걸 것인가"에 더 집중할 수 있다.
     */

    private final RedissonClient redissonClient;

    @Override
    public void lock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        // Redisson은 키마다 RLock 프록시를 제공하고,
        // tryLock이 waitTime 동안 대기 후 성공 여부를 boolean으로 돌려준다.
        RLock rLock = redissonClient.getLock(key);
        try {
            boolean acquired = rLock.tryLock(waitTime, leaseTime, timeUnit);
            if (!acquired) {
                throw new LockException("락 획득 실패 [Redisson]: key=" + key);
            }
            log.debug("락 획득 성공 [Redisson]: key={}", key);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockException("락 획득 중 인터럽트 발생 [Redisson]: key=" + key);
        }
    }

    @Override
    public void unlock(String key) {
        RLock rLock = redissonClient.getLock(key);
        // 현재 스레드가 실제 소유자인 경우에만 unlock 해야
        // lease 만료 후 다른 요청이 잡은 락을 잘못 푸는 일을 막을 수 있다.
        if (rLock.isHeldByCurrentThread()) {
            rLock.unlock();
            log.debug("락 해제 완료 [Redisson]: key={}", key);
        }
    }
}
