package com.project.wms.interfaces.rest.inventory

import com.project.wms.support.IntegrationTestBase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import java.util.UUID
import kotlin.test.assertEquals

/**
 * 실제 Redis로 adjust 요청-단위 멱등(Idempotency-Key)을 검증한다. (seed: id 1~3)
 */
@Disabled(IntegrationTestBase.DISABLED_REASON)
class AdjustIdempotencyIntegrationTest : IntegrationTestBase() {

    @Value("\${local.server.port}")
    private var port: Int = 0

    private val client: RestClient by lazy { RestClient.create("http://localhost:$port") }

    private fun quantityOf(id: Long): Int =
        client.get().uri("/api/v1/inventory/$id")
            .retrieve().body(InventoryResponse::class.java)!!.quantity

    private fun adjust(id: Long, delta: Int, idempotencyKey: String?) {
        client.post().uri("/api/v1/inventory/$id/adjust")
            .contentType(MediaType.APPLICATION_JSON)
            .apply { if (idempotencyKey != null) header("Idempotency-Key", idempotencyKey) }
            .body("""{"delta": $delta}""")
            .retrieve().toBodilessEntity()
    }

    @Test
    fun `같은 Idempotency-Key로 adjust를 두 번 보내도 한 번만 적용된다`() {
        val before = quantityOf(3L)
        val key = "it-adjust-${UUID.randomUUID()}"

        repeat(2) { adjust(3L, delta = 7, idempotencyKey = key) }

        assertEquals(before + 7, quantityOf(3L))
    }

    @Test
    fun `키 없이 adjust를 두 번 보내면 두 번 모두 적용된다`() {
        val before = quantityOf(3L)

        repeat(2) { adjust(3L, delta = 1, idempotencyKey = null) }

        assertEquals(before + 2, quantityOf(3L))
    }
}
