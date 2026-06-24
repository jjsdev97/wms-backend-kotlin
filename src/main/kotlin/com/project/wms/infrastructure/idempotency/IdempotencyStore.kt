package com.project.wms.infrastructure.idempotency

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Idempotency-Key 상태를 Redis에 저장한다.
 *
 * 키는 실행별 소유권 토큰이 포함된 처리 중 마커로 선점된다. 완료·해제는 Lua
 * script에서 현재 마커가 자신의 토큰과 일치할 때만 수행해, TTL 만료 후 키를
 * 재선점한 다른 실행의 상태를 이전 실행이 덮어쓰거나 삭제하지 못하게 한다.
 */
@Component
class IdempotencyStore(private val redis: StringRedisTemplate) {

    /** [key]를 처리 중 상태로 선점한다. 이미 존재하면 false. */
    fun acquire(key: String, ownerToken: String, ttlSeconds: Long): Boolean =
        redis.opsForValue()
            .setIfAbsent(redisKey(key), inProgressValue(ownerToken), Duration.ofSeconds(ttlSeconds)) == true

    /** [ownerToken]이 현재 소유자일 때만 완료 응답으로 교체한다. */
    fun complete(key: String, ownerToken: String, payload: String, ttlSeconds: Long): Boolean =
        redis.execute(
            COMPLETE_IF_OWNER,
            listOf(redisKey(key)),
            inProgressValue(ownerToken),
            payload,
            ttlSeconds.toString(),
        ) == 1L

    /** [ownerToken]이 현재 소유자일 때만 선점을 해제한다. */
    fun release(key: String, ownerToken: String): Boolean =
        redis.execute(
            RELEASE_IF_OWNER,
            listOf(redisKey(key)),
            inProgressValue(ownerToken),
        ) == 1L

    /** 현재 저장된 값. 없으면 null, 처리 중이면 소유 토큰 마커, 완료면 응답 본문. */
    fun find(key: String): String? =
        redis.opsForValue().get(redisKey(key))

    fun isInProgress(value: String?): Boolean = value?.startsWith(IN_PROGRESS_PREFIX) == true

    private fun inProgressValue(ownerToken: String) = "$IN_PROGRESS_PREFIX$ownerToken"

    private fun redisKey(key: String) = "$KEY_PREFIX$key"

    companion object {
        const val KEY_PREFIX = "idempotency:v2:"
        const val IN_PROGRESS_PREFIX = "__IN_PROGRESS__:"

        private val COMPLETE_IF_OWNER = DefaultRedisScript(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                redis.call('set', KEYS[1], ARGV[2], 'EX', ARGV[3])
                return 1
            end
            return 0
            """.trimIndent(),
            Long::class.java,
        )

        private val RELEASE_IF_OWNER = DefaultRedisScript(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """.trimIndent(),
            Long::class.java,
        )
    }
}
