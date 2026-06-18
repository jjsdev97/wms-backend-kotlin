package com.project.wms.infrastructure.idempotency

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Idempotency-Key 상태를 Redis에 저장한다.
 *
 * 키는 "처리 중(IN_PROGRESS)" 마커로 선점된 뒤, 완료 시 직렬화된 응답
 * 본문으로 덮어쓰여진다. 응답 본문은 항상 JSON이므로 마커와 구분된다.
 */
@Component
class IdempotencyStore(private val redis: StringRedisTemplate) {

    /** [key]를 처리 중 상태로 선점한다. 이미 존재하면 false. */
    fun acquire(key: String, ttlSeconds: Long): Boolean =
        redis.opsForValue()
            .setIfAbsent(redisKey(key), IN_PROGRESS, Duration.ofSeconds(ttlSeconds)) == true

    /** 선점한 [key]에 완료된 응답 본문을 저장한다. */
    fun complete(key: String, payload: String, ttlSeconds: Long) {
        redis.opsForValue().set(redisKey(key), payload, Duration.ofSeconds(ttlSeconds))
    }

    /** 처리 실패 시 선점을 해제해 재시도를 허용한다. */
    fun release(key: String) {
        redis.delete(redisKey(key))
    }

    /** 현재 저장된 값. 없으면 null, 처리 중이면 [IN_PROGRESS], 완료면 응답 본문. */
    fun find(key: String): String? =
        redis.opsForValue().get(redisKey(key))

    fun isInProgress(value: String?): Boolean = value == IN_PROGRESS

    private fun redisKey(key: String) = "idempotency:$key"

    companion object {
        const val IN_PROGRESS = "__IN_PROGRESS__"
    }
}
