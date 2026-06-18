package com.project.wms.infrastructure.idempotency

import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * 전송 프로토콜에 독립적인 멱등 실행기. REST(헤더)·MCP(toolParam) 등 어느
 * 어댑터든 멱등 키와 실행 블록만 넘기면 재사용한다.
 *
 * - [key]가 null/blank: 멱등성 없이 [block] 실행.
 * - 첫 요청: 키를 선점하고 [block] 실행 후 결과를 캐시.
 * - 재요청(완료): 캐시된 응답을 [type]으로 역직렬화해 반환([block] 미실행).
 * - 재요청(처리 중): [IdempotencyConflictException].
 *
 * @param scope 서로 다른 작업 간 키 충돌을 막기 위한 네임스페이스(예: "rest:POST:/.../reserve", "mcp:reserveStock").
 */
@Component
class IdempotencyExecutor(
    private val store: IdempotencyStore,
    private val objectMapper: ObjectMapper,
) {

    fun <T> execute(key: String?, scope: String, type: Class<T>, ttlSeconds: Long, block: () -> T): T {
        if (key.isNullOrBlank()) {
            return block()
        }
        val scopedKey = "$scope:$key"

        if (store.acquire(scopedKey, ttlSeconds)) {
            return try {
                val result = block()
                store.complete(scopedKey, objectMapper.writeValueAsString(result), ttlSeconds)
                result
            } catch (e: Throwable) {
                store.release(scopedKey)
                throw e
            }
        }

        val cached = store.find(scopedKey)
        if (cached == null || store.isInProgress(cached)) {
            throw IdempotencyConflictException(key)
        }
        return objectMapper.readValue(cached, type)
    }

    companion object {
        /** 멱등 키 보관 기본 시간(초). 24시간. */
        const val DEFAULT_TTL_SECONDS = 86_400L
    }
}
