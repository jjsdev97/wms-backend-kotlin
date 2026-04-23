package com.project.wms.application.inventory

import com.project.wms.domain.inventory.AdjustStockCommand
import com.project.wms.domain.inventory.InventoryItem
import com.project.wms.domain.inventory.InventoryNotFoundException
import com.project.wms.domain.inventory.InsufficientStockException
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

    private fun item(quantity: Int) = InventoryItem(
        id = 1L,
        sku = "SKU-001",
        warehouseId = "WH-A",
        quantity = quantity
    )
}
