package com.switchwon.forexordersystem.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 컨트롤러, 서비스 로깅 Aspect
 * DEBUG 레벨에서만 상세 출력 (dev: DEBUG, prod: WARN)
 * 처리시간(ms) 자동 측정
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    // 컨트롤러
    @Pointcut("execution(public * com.switchwon.forexordersystem..controller..*(..))")
    public void controllerLayer() {}

    // 서비스
    @Pointcut("execution(public * com.switchwon.forexordersystem..service..*(..))")
    public void serviceLayer() {}

    @Around("controllerLayer() || serviceLayer()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        long startedAt = System.currentTimeMillis();

        if (log.isDebugEnabled()) {
            log.debug("===>>> {} args={}", signature, Arrays.toString(joinPoint.getArgs()));
        }

        try {
            Object result = joinPoint.proceed();
            long took = System.currentTimeMillis() - startedAt;
            if (log.isDebugEnabled()) {
                log.debug("<<<=== {} done in {}ms", signature, took);
            }
            return result;
        } catch (Throwable t) {
            long took = System.currentTimeMillis() - startedAt;
            log.warn("((( X ))) {} failed in {}ms - {}: {}", signature, took, t.getClass().getSimpleName(), t.getMessage());
            throw t;
        }
    }
}