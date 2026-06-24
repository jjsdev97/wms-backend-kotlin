package com.project.wms.infrastructure.idempotency

/**
 * `Idempotency-Key` 헤더 기반 멱등 처리를 적용한다.
 *
 * 헤더가 없으면 멱등성 없이 그대로 실행되고, 헤더가 있으면 동일 키의
 * 첫 요청만 실제로 처리한 뒤 응답을 캐시한다. 같은 키의 재요청은 캐시된
 * 응답을 반환하며, 아직 처리 중이면 409로 거절한다. 같은 키로 **다른 내용**의
 * 요청이 오면(키 재사용) 409로 거절한다 — 핸들러 인자가 지문으로 검증된다.
 *
 * @param ttlSeconds 키 보관 시간(초). 기본 24시간.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Idempotent(val ttlSeconds: Long = IdempotencyExecutor.DEFAULT_TTL_SECONDS)
