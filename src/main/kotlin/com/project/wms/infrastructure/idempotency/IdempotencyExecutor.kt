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
 * 선점 마커(IN_PROGRESS)는 응답 캐시보다 짧은 [IN_PROGRESS_TTL_SECONDS]로 건다.
 * 처리 중 프로세스가 죽어 [release]가 돌지 못해도 마커가 곧 만료돼 재시도가 풀리도록.
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

        if (store.acquire(scopedKey, IN_PROGRESS_TTL_SECONDS)) {
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

        /**
         * 선점 마커 보관 시간(초). 정상 처리 시간을 덮을 만큼 넉넉하되,
         * 크래시로 [IdempotencyStore.release]가 누락돼도 곧 풀리도록 짧게 둔다.
         */
        const val IN_PROGRESS_TTL_SECONDS = 60L
    }
}
