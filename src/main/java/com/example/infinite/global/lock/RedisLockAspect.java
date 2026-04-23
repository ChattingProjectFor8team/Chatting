package com.example.infinite.global.lock;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RedisLockAspect {

    /**
     * @RedisLock 메서드 주위를 감싸 분산 락을 자동 적용하는 AOP 계층이다.
     *
     * 학습 포인트:
     * - 서비스 메서드는 비즈니스 로직만 유지하고
     * - 락 획득/해제는 aspect가 공통으로 처리한다.
     *
     * 즉 "락 기술 코드"와 "도메인 코드"를 분리하는 장치다.
     */

    private final LockService lockService;
    private final ExpressionParser parser = new SpelExpressionParser();

    // @Qualifier를 생성자 파라미터에 직접 명시 (Lombok @RequiredArgsConstructor 미사용)
    // Lombok은 @Qualifier를 생성자 파라미터에 복사하지 않으므로 직접 작성
    public RedisLockAspect(@Qualifier("redissonLockService") LockService lockService) {
        this.lockService = lockService;
    }

    @Around("@annotation(redisLock)")
    public Object around(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        // SpEL로 메서드 인자에서 실제 락 키를 계산한다.
        // 예: "'artist-post:like:' + #artistPostId + ':member:' + #memberId"
        String key = resolveKey(joinPoint, redisLock.key());

        lockService.lock(key, redisLock.waitTime(), redisLock.leaseTime(), redisLock.timeUnit());
        try {
            return joinPoint.proceed();
        } finally {
            // 비즈니스 예외가 나도 finally에서 해제해 락 누수를 막는다.
            lockService.unlock(key);
        }
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        // 메서드 파라미터 이름을 SpEL 변수로 바인딩해 annotation 문자열을 실제 키로 바꾼다.
        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }
}
