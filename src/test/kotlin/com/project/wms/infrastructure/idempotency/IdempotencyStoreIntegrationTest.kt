package com.project.wms.infrastructure.idempotency

import com.project.wms.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IdempotencyStoreIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var store: IdempotencyStore

    @Test
    fun `TTL 만료 후 재선점되면 이전 소유자는 새 소유자의 상태를 완료하거나 해제할 수 없다`() {
        val key = "ownership-${UUID.randomUUID()}"
        val firstOwner = "first-${UUID.randomUUID()}"
        val secondOwner = "second-${UUID.randomUUID()}"

        assertTrue(store.acquire(key, firstOwner, ttlSeconds = 1))
        waitUntilExpired(key)
        assertTrue(store.acquire(key, secondOwner, ttlSeconds = 30))

        assertFalse(store.complete(key, firstOwner, """{"owner":"first"}""", ttlSeconds = 30))
        assertFalse(store.release(key, firstOwner))
        assertTrue(store.isInProgress(store.find(key)))

        assertTrue(store.complete(key, secondOwner, """{"owner":"second"}""", ttlSeconds = 30))
        assertEquals("""{"owner":"second"}""", store.find(key))
    }

    private fun waitUntilExpired(key: String) {
        val deadline = System.nanoTime() + 3_000_000_000L
        while (store.find(key) != null && System.nanoTime() < deadline) {
            Thread.sleep(25)
        }
        assertEquals(null, store.find(key), "Redis 선점 키가 제한 시간 안에 만료되지 않음")
    }
}
