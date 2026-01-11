package com.communitcation.rest.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequestLockAspect {

    private final Map<String, ReentrantLock> inProgressLock;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(reqeustLock)")
    public Object around(ProceedingJoinPoint joinPoint, ReqeustLock reqeustLock) throws Throwable {
        String sessionId = parseSpEL(joinPoint, reqeustLock.sessionKey());

        long timeout = reqeustLock.timeout();

        ReentrantLock lock = inProgressLock.computeIfAbsent(sessionId, k -> new ReentrantLock());

        boolean acquired = false;

        try {
            acquired = lock.tryLock(timeout, TimeUnit.SECONDS);

            if (!acquired) {
                throw new RuntimeException("이미지를 생성하는 중입니다. 잠시 후 다시 시도해주세요.");
            }

            return joinPoint.proceed();

        }
        finally {
            if (acquired) {
                lock.unlock();
            }

            if (!lock.hasQueuedThreads()) {
                inProgressLock.remove(sessionId);
            }
        }
    }

    private String parseSpEL(ProceedingJoinPoint joinPoint, String expression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                null, method, args, parameterNameDiscoverer
        );

        return parser.parseExpression(expression).getValue(context, String.class);
    }
}
