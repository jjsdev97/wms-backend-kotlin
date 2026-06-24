package com.project.wms.infrastructure.idempotency

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals

class IdempotencyExecutorTest {

    private val store = mockk<IdempotencyStore>(relaxed = true)
    private val objectMapper = jacksonObjectMapper()
    private val executor = IdempotencyExecutor(store, objectMapper)

    private val ttl = 86_400L
    private val inProgressTtl = IdempotencyExecutor.IN_PROGRESS_TTL_SECONDS
    private val scope = "rest:POST:/api/v1/inventory/1/reserve"

    data class Dummy(val id: Long, val name: String)

    @Test
    fun `키가 없으면 멱등 처리 없이 블록을 실행한다`() {
        var executed = false
        val result = executor.execute(key = null, scope, Dummy::class.java, ttl) {
            executed = true
            Dummy(1, "a")
        }

        assertEquals(Dummy(1, "a"), result)
        assert(executed)
        verify(exactly = 0) { store.acquire(any(), any(), any()) }
    }

    /** 캐시에 저장되는 봉투(요청 지문 + 직렬화된 응답)의 JSON. */
    private fun envelopeJson(fingerprint: String?, body: Any): String =
        objectMapper.writeValueAsString(
            IdempotencyExecutor.Envelope(fingerprint, objectMapper.writeValueAsString(body))
        )

    @Test
    fun `첫 요청이면 블록을 실행하고 응답을 캐시한다`() {
        val expected = Dummy(1, "a")
        val ownerToken = slot<String>()
        every { store.acquire("$scope:k1", capture(ownerToken), inProgressTtl) } returns true
        every { store.complete("$scope:k1", any(), any(), ttl) } returns true

        val result = executor.execute("k1", scope, Dummy::class.java, ttl, fingerprint = "fp") { expected }

        assertEquals(expected, result)
        verify { store.complete("$scope:k1", ownerToken.captured, envelopeJson("fp", expected), ttl) }
    }

    @Test
    fun `완료된 키 재요청이면 캐시된 응답을 반환하고 블록을 실행하지 않는다`() {
        val cached = Dummy(7, "cached")
        every { store.acquire(any(), any(), inProgressTtl) } returns false
        every { store.find("$scope:k1") } returns envelopeJson("fp", cached)
        every { store.isInProgress(any()) } returns false

        var executed = false
        val result = executor.execute("k1", scope, Dummy::class.java, ttl, fingerprint = "fp") {
            executed = true
            Dummy(1, "fresh")
        }

        assertEquals(cached, result)
        assert(!executed)
    }

    @Test
    fun `같은 키에 다른 지문이면 키 재사용 예외를 던진다`() {
        val cached = Dummy(7, "cached")
        every { store.acquire(any(), any(), inProgressTtl) } returns false
        every { store.find("$scope:k1") } returns envelopeJson("fp-first", cached)
        every { store.isInProgress(any()) } returns false

        assertThrows<IdempotencyKeyReuseException> {
            executor.execute("k1", scope, Dummy::class.java, ttl, fingerprint = "fp-second") {
                Dummy(1, "fresh")
            }
        }
    }

    @Test
    fun `처리 중인 키 재요청이면 충돌 예외를 던진다`() {
        val inProgress = "${IdempotencyStore.IN_PROGRESS_PREFIX}owner"
        every { store.acquire(any(), any(), inProgressTtl) } returns false
        every { store.find(any()) } returns inProgress
        every { store.isInProgress(inProgress) } returns true

        assertThrows<IdempotencyConflictException> {
            executor.execute("k1", scope, Dummy::class.java, ttl) { Dummy(1, "a") }
        }
    }

    @Test
    fun `블록 실행 중 예외가 나면 선점을 해제하고 예외를 전파한다`() {
        val ownerToken = slot<String>()
        every { store.acquire("$scope:k1", capture(ownerToken), inProgressTtl) } returns true

        assertThrows<IllegalStateException> {
            executor.execute("k1", scope, Dummy::class.java, ttl) { throw IllegalStateException("boom") }
        }

        verify { store.release("$scope:k1", ownerToken.captured) }
        verify(exactly = 0) { store.complete(any(), any(), any(), any()) }
    }

    @Test
    fun `실행 완료 전에 소유권을 잃으면 결과를 덮어쓰지 않고 충돌을 반환한다`() {
        val ownerToken = slot<String>()
        every { store.acquire("$scope:k1", capture(ownerToken), inProgressTtl) } returns true
        every { store.complete("$scope:k1", any(), any(), ttl) } returns false

        assertThrows<IdempotencyConflictException> {
            executor.execute("k1", scope, Dummy::class.java, ttl) { Dummy(1, "a") }
        }

        verify { store.complete("$scope:k1", ownerToken.captured, any(), ttl) }
        verify { store.release("$scope:k1", ownerToken.captured) }
    }
}
