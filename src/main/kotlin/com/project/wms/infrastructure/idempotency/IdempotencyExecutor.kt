package com.project.wms.infrastructure.idempotency

import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.UUID

/**
 * 전송 프로토콜에 독립적인 멱등 실행기. REST(헤더)·MCP(toolParam) 등 어느
 * 어댑터든 멱등 키와 실행 블록만 넘기면 재사용한다.
 *
 * - [key]가 null/blank: 멱등성 없이 [block] 실행.
 * - 첫 요청: 실행별 소유권 토큰으로 키를 선점하고 [block] 실행 후
 *   자신이 여전히 소유자일 때만 (요청 지문 + 응답)을 캐시.
 * - 재요청(완료): 캐시된 응답을 [type]으로 역직렬화해 반환([block] 미실행).
 *   단 [fingerprint]가 첫 요청과 다르면(같은 키 다른 내용) [IdempotencyKeyReuseException].
 * - 재요청(처리 중): [IdempotencyConflictException].
 *
 * 선점 마커(IN_PROGRESS)는 응답 캐시보다 짧은 [IN_PROGRESS_TTL_SECONDS]로 건다.
 * 처리 중 프로세스가 죽어 [release]가 돌지 못해도 마커가 곧 만료돼 재시도가 풀리도록.
 *
 * 한계(정확히 한 번이 아님): [block]의 부수효과(예: DB 커밋)와 응답 캐시 저장은
 * 서로 다른 저장소라 원자적이지 않다. [block]은 성공했는데 캐시 저장 직전 프로세스가
 * 죽으면, IN_PROGRESS 마커가 만료된 뒤 재시도가 부수효과를 **다시** 적용할 수 있다.
 * (분산 멱등의 본질적 한계 — at-least-once에 가깝다. 엄밀한 exactly-once가 필요하면
 *  부수효과와 멱등 상태를 같은 트랜잭션 경계에 두는 outbox 등으로 전환해야 한다.)
 *
 * @param scope 서로 다른 작업 간 키 충돌을 막기 위한 네임스페이스(예: "rest:POST:/.../reserve", "mcp:adjustStock").
 * @param fingerprint 요청 내용 지문(선택). 같은 키 재요청의 내용 일치를 검증한다. null이면 검증하지 않는다.
 */
@Component
class IdempotencyExecutor(
    private val store: IdempotencyStore,
    private val objectMapper: ObjectMapper,
) {

    fun <T> execute(
        key: String?,
        scope: String,
        type: Class<T>,
        ttlSeconds: Long,
        fingerprint: String? = null,
        block: () -> T,
    ): T {
        if (key.isNullOrBlank()) {
            return block()
        }
        val scopedKey = "$scope:$key"

        val ownerToken = UUID.randomUUID().toString()
        if (store.acquire(scopedKey, ownerToken, IN_PROGRESS_TTL_SECONDS)) {
            return try {
                val result = block()
                val envelope = Envelope(fingerprint, objectMapper.writeValueAsString(result))
                val completed = store.complete(
                    scopedKey,
                    ownerToken,
                    objectMapper.writeValueAsString(envelope),
                    ttlSeconds,
                )
                if (!completed) {
                    throw IdempotencyConflictException(key)
                }
                result
            } catch (e: Throwable) {
                store.release(scopedKey, ownerToken)
                throw e
            }
        }

        val cached = store.find(scopedKey)
        if (cached == null || store.isInProgress(cached)) {
            throw IdempotencyConflictException(key)
        }
        val envelope = objectMapper.readValue(cached, Envelope::class.java)
        if (fingerprint != null && envelope.fingerprint != fingerprint) {
            throw IdempotencyKeyReuseException(key)
        }
        return objectMapper.readValue(envelope.body, type)
    }

    /** 캐시 봉투: 요청 지문과 직렬화된 응답 본문을 함께 보관해 키 재사용을 검증한다. */
    data class Envelope(val fingerprint: String?, val body: String)

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
