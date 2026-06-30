package com.project.wms.infrastructure.security

import org.springframework.stereotype.Component
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

@Component
class AuthRateLimiter {
    private val clock: Clock = Clock.systemUTC()
    private val attempts = ConcurrentHashMap<String, Window>()

    fun check(key: String) {
        val now = clock.millis()
        val window = attempts.compute(key) { _, current ->
            if (current == null || now >= current.resetAtMillis) {
                Window(count = 1, resetAtMillis = now + WINDOW_MILLIS)
            } else {
                current.copy(count = current.count + 1)
            }
        } ?: error("rate limit window update failed")

        if (window.count > MAX_REQUESTS_PER_WINDOW) {
            throw AuthRateLimitException()
        }
    }

    private data class Window(
        val count: Int,
        val resetAtMillis: Long,
    )

    companion object {
        private const val MAX_REQUESTS_PER_WINDOW = 20
        private const val WINDOW_MILLIS = 60_000L
    }
}
