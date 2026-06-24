package com.project.wms.interfaces.rest.inventory

import com.project.wms.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 실제 Redis로 adjust 요청-단위 멱등(Idempotency-Key)을 검증한다. (seed: id 1~3)
 */
class AdjustIdempotencyIntegrationTest : IntegrationTestBase() {

    @Value("\${local.server.port}")
    private var port: Int = 0

    private val client: RestClient by lazy { RestClient.create("http://localhost:$port") }

    // adjust(재고 쓰기)는 JWT 보호 경로다. 실제 흐름(signup→login)으로 토큰을 받아
    // Bearer로 실어 보낸다. (username은 충돌 방지를 위해 매 실행 유니크하게)
    private val token: String by lazy {
        val username = "it-user-${UUID.randomUUID()}"
        val password = "it-pw-12345678"
        client.post().uri("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"username": "$username", "password": "$password"}""")
            .retrieve().toBodilessEntity()
        client.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"username": "$username", "password": "$password"}""")
            .retrieve().body(Map::class.java)!!["accessToken"] as String
    }

    private fun quantityOf(id: Long): Int =
        client.get().uri("/api/v1/inventory/$id")
            .retrieve().body(InventoryResponse::class.java)!!.quantity

    private fun adjust(id: Long, delta: Int, idempotencyKey: String?) {
        client.post().uri("/api/v1/inventory/$id/adjust")
            .header("Authorization", "Bearer $token")
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

    @Test
    fun `같은 Idempotency-Key로 다른 delta를 보내면 409로 거절된다`() {
        val key = "it-adjust-reuse-${UUID.randomUUID()}"
        val before = quantityOf(3L)
        adjust(3L, delta = 5, idempotencyKey = key) // 첫 요청만 적용

        val ex = assertFailsWith<HttpClientErrorException> {
            adjust(3L, delta = 9, idempotencyKey = key) // 같은 키, 다른 내용 → 거절
        }

        assertEquals(409, ex.statusCode.value())
        assertEquals(before + 5, quantityOf(3L)) // 두 번째는 적용되지 않음
    }
}
