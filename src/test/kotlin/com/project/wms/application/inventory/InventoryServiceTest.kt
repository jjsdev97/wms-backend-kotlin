package com.project.wms.application.inventory

import com.project.wms.domain.inventory.AdjustStockCommand
import com.project.wms.domain.inventory.InventoryItem
import com.project.wms.domain.inventory.InventoryNotFoundException
import com.project.wms.domain.inventory.InsufficientReservationException
import com.project.wms.domain.inventory.InsufficientStockException
import com.project.wms.domain.inventory.InvalidAmountException
import com.project.wms.domain.inventory.ReservationCommand
import com.project.wms.domain.port.InventoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class InventoryServiceTest {

    private val repository = mockk<InventoryRepository>()
    private val service = InventoryService(repository)

    @Test
    fun `재고 단건 조회 성공`() {
        val item = item(quantity = 100)
        every { repository.findById(1L) } returns item

        val result = service.getInventory(1L)

        assertEquals("SKU-001", result.sku)
        assertEquals(100, result.quantity)
    }

    @Test
    fun `존재하지 않는 재고 조회 시 InventoryNotFoundException`() {
        every { repository.findById(999L) } returns null

        assertThrows<InventoryNotFoundException> { service.getInventory(999L) }
    }

    @Test
    fun `재고 증가`() {
        val item = item(quantity = 100)
        every { repository.findById(1L) } returns item
        every { repository.save(any()) } returnsArgument 0

        val result = service.adjustStock(AdjustStockCommand(1L, delta = 50))

        assertEquals(150, result.quantity)
        verify(exactly = 1) { repository.save(item) }
    }

    @Test
    fun `재고 차감`() {
        val item = item(quantity = 100)
        every { repository.findById(1L) } returns item
        every { repository.save(any()) } returnsArgument 0

        val result = service.adjustStock(AdjustStockCommand(1L, delta = -30))

        assertEquals(70, result.quantity)
    }

    @Test
    fun `재고 부족 시 InsufficientStockException`() {
        val item = item(quantity = 10)
        every { repository.findById(1L) } returns item

        assertThrows<InsufficientStockException> {
            service.adjustStock(AdjustStockCommand(1L, delta = -20))
        }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `정확히 남은 수량만큼 차감하면 0이 된다`() {
        val item = item(quantity = 50)
        every { repository.findById(1L) } returns item
        every { repository.save(any()) } returnsArgument 0

        val result = service.adjustStock(AdjustStockCommand(1L, delta = -50))

        assertEquals(0, result.quantity)
    }

    @Test
    fun `예약 시 reserved 증가하고 available 감소`() {
        val item = item(quantity = 100)
        every { repository.findById(1L) } returns item
        every { repository.save(any()) } returnsArgument 0

        val result = service.reserve(ReservationCommand(1L, amount = 30))

        assertEquals(100, result.quantity)
        assertEquals(30, result.reserved)
        assertEquals(70, result.available)
    }

    @Test
    fun `가용 수량 초과 예약 시 InsufficientStockException`() {
        val item = item(quantity = 100, reserved = 80)
        every { repository.findById(1L) } returns item

        assertThrows<InsufficientStockException> {
            service.reserve(ReservationCommand(1L, amount = 30))
        }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `확정 시 quantity와 reserved 모두 차감`() {
        val item = item(quantity = 100, reserved = 40)
        every { repository.findById(1L) } returns item
        every { repository.save(any()) } returnsArgument 0

        val result = service.confirm(ReservationCommand(1L, amount = 40))

        assertEquals(60, result.quantity)
        assertEquals(0, result.reserved)
    }

    @Test
    fun `예약분 초과 확정 시 InsufficientReservationException`() {
        val item = item(quantity = 100, reserved = 10)
        every { repository.findById(1L) } returns item

        assertThrows<InsufficientReservationException> {
            service.confirm(ReservationCommand(1L, amount = 20))
        }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `취소 시 reserved만 감소하고 quantity 유지`() {
        val item = item(quantity = 100, reserved = 40)
        every { repository.findById(1L) } returns item
        every { repository.save(any()) } returnsArgument 0

        val result = service.cancel(ReservationCommand(1L, amount = 40))

        assertEquals(100, result.quantity)
        assertEquals(0, result.reserved)
    }

    @Test
    fun `0 이하 수량 예약 시 InvalidAmountException`() {
        val item = item(quantity = 100)
        every { repository.findById(1L) } returns item

        assertThrows<InvalidAmountException> {
            service.reserve(ReservationCommand(1L, amount = 0))
        }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `예약분이 있으면 그 아래로 총 재고를 차감할 수 없다`() {
        val item = item(quantity = 100, reserved = 80)
        every { repository.findById(1L) } returns item

        assertThrows<InsufficientStockException> {
            service.adjustStock(AdjustStockCommand(1L, delta = -30))
        }
        verify(exactly = 0) { repository.save(any()) }
    }

    private fun item(quantity: Int, reserved: Int = 0) = InventoryItem(
        id = 1L,
        sku = "SKU-001",
        warehouseId = "WH-A",
        quantity = quantity,
        reserved = reserved
    )
}
