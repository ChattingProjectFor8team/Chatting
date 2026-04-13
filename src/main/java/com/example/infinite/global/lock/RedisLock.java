package com.example.infinite.global.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {

    /**
     * 락 키. SpEL 표현식 사용 가능.
     * 예: "'raffle:' + #raffleId"
     */
    String key();

    /**
     * 락 획득 대기 시간 (기본 5초)
     */
    long waitTime() default 5;

    /**
     * 락 점유 시간 (기본 3초)
     * -1로 설정하면 Redisson Watchdog이 자동 연장
     */
    long leaseTime() default 3;

    /**
     * 시간 단위 (기본 SECONDS)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
