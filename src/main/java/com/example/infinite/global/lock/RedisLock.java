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
     * 락 점유 시간 (기본 10초)
     * -1로 설정하면 Redisson Watchdog이 자동 연장 (비정상 종료 시 30초간 데드락 주의)
     * 외부 API 호출 등 소요 시간이 불확실한 경우에만 -1 사용 권장
     */
    long leaseTime() default 10;

    /**
     * 시간 단위 (기본 SECONDS)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
