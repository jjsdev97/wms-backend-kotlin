package com.project.wms.infrastructure.idempotency

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * REST 어댑터용 멱등 처리. [Idempotent]가 붙은 핸들러를 `Idempotency-Key`
 * 헤더 기준으로 [IdempotencyExecutor]에 위임한다. 실제 선점·캐시 로직은
 * 프로토콜 독립적인 executor가 담당하며, 이 Aspect는 HTTP 헤더 추출과
 * REST 응답 타입 전달만 책임진다.
 */
@Aspect
@Component
class IdempotencyAspect(private val executor: IdempotencyExecutor) {

    @Around("@annotation(idempotent)")
    fun handle(joinPoint: ProceedingJoinPoint, idempotent: Idempotent): Any? {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        val key = request?.getHeader(HEADER)
        // 메서드+경로로 스코프를 좁혀 서로 다른 엔드포인트 간 키 충돌을 방지한다.
        val scope = "rest:${request?.method}:${request?.requestURI}"
        @Suppress("UNCHECKED_CAST")
        val returnType = (joinPoint.signature as MethodSignature).returnType as Class<Any>

        return executor.execute(key, scope, returnType, idempotent.ttlSeconds) {
            joinPoint.proceed()
        }
    }

    companion object {
        const val HEADER = "Idempotency-Key"
    }
}
