package com.project.wms.application.inventory

import com.project.wms.domain.inventory.ReservationRef
import com.project.wms.domain.inventory.ReserveCommand
import com.project.wms.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals

/**
 * 실제 Postgres로 예약 자연키 멱등성을 검증한다. (seed: id 1~3)
 */
class ReservationIdempotencyIntegrationTest @Autowired constructor(
    private val service: InventoryService,
) : IntegrationTestBase() {

    @Test
    fun `같은 reservationId로 두 번 예약해도 재고는 한 번만 차감된다`() {
        val before = service.getInventory(1L).reserved
        val command = ReserveCommand(1L, "it-reserve-${UUID.randomUUID()}", 5)

        service.reserve(command)
        service.reserve(command) // 멱등 재호출

        assertEquals(before + 5, service.getInventory(1L).reserved)
    }

    @Test
    fun `확정은 멱등하다 — 두 번 확정해도 한 번만 차감`() {
        val reservationId = "it-confirm-${UUID.randomUUID()}"
        service.reserve(ReserveCommand(2L, reservationId, 10))
        val quantityBefore = service.getInventory(2L).quantity

        service.confirm(ReservationRef(2L, reservationId))
        service.confirm(ReservationRef(2L, reservationId)) // 멱등 재호출

        val item = service.getInventory(2L)
        assertEquals(quantityBefore - 10, item.quantity)
    }

    @Test
    fun `취소는 멱등하다 — 두 번 취소해도 예약분은 한 번만 해제`() {
        val reservationId = "it-cancel-${UUID.randomUUID()}"
        service.reserve(ReserveCommand(3L, reservationId, 4))
        val reservedAfterReserve = service.getInventory(3L).reserved

        service.cancel(ReservationRef(3L, reservationId))
        service.cancel(ReservationRef(3L, reservationId)) // 멱등 재호출

        assertEquals(reservedAfterReserve - 4, service.getInventory(3L).reserved)
    }
}
