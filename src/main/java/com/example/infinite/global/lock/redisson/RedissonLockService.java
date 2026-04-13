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

    private final RedissonClient redissonClient;

    @Override
    public void lock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
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
        if (rLock.isHeldByCurrentThread()) {
            rLock.unlock();
            log.debug("락 해제 완료 [Redisson]: key={}", key);
        }
    }
}
