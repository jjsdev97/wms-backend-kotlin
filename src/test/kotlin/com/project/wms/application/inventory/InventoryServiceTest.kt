package com.project.wms.application.inventory

import com.project.wms.domain.inventory.AdjustStockCommand
import com.project.wms.domain.inventory.IllegalReservationStateException
import com.project.wms.domain.inventory.InsufficientStockException
import com.project.wms.domain.inventory.InvalidAmountException
import com.project.wms.domain.inventory.InventoryItem
import com.project.wms.domain.inventory.InventoryNotFoundException
import com.project.wms.domain.inventory.Reservation
import com.project.wms.domain.inventory.ReservationConflictException
import com.project.wms.domain.inventory.ReservationNotFoundException
import com.project.wms.domain.inventory.ReservationRef
import com.project.wms.domain.inventory.ReserveCommand
import com.project.wms.domain.port.InventoryRepository
import com.project.wms.domain.port.ReservationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class InventoryServiceTest {

    private val repository = mockk<InventoryRepository>()
    private val reservationRepository = mockk<ReservationRepository>()
    private val service = InventoryService(repository, reservationRepository)

    // --- 조회 / 증감 ---

    @Test
    fun `재고 단건 조회 성공`() {
        every { repository.findById(1L) } returns item(quantity = 100)

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
    fun `재고 부족 시 InsufficientStockException`() {
        every { repository.findById(1L) } returns item(quantity = 10)

        assertThrows<InsufficientStockException> {
            service.adjustStock(AdjustStockCommand(1L, delta = -20))
        }
        verify(exactly = 0) { repository.save(any()) }
    }

    // --- 예약 (reservationId 자연키 멱등) ---

    @Test
    fun `신규 예약 시 reserved 증가하고 예약을 저장한다`() {
        val item = item(quantity = 100)
        every { reservationRepository.findById("r1") } returns null
        every { repository.findById(1L) } returns item
        every { reservationRepository.save(any()) } returnsArgument 0
        every { repository.save(any()) } returnsArgument 0

        val result = service.reserve(ReserveCommand(1L, "r1", amount = 30))

        assertEquals(30, result.reserved)
        assertEquals(70, result.available)
        verify(exactly = 1) { reservationRepository.save(any()) }
    }

    @Test
    fun `같은 reservationId 재호출은 중복 예약하지 않는다(멱등)`() {
        val item = item(quantity = 100, reserved = 30)
        every { reservationRepository.findById("r1") } returns Reservation("r1", 1L, 30)
        every { repository.findById(1L) } returns item

        val result = service.reserve(ReserveCommand(1L, "r1", amount = 30))

        assertEquals(30, result.reserved)
        verify(exactly = 0) { reservationRepository.save(any()) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `같은 reservationId에 다른 내용이면 ReservationConflictException`() {
        every { reservationRepository.findById("r1") } returns Reservation("r1", 1L, 30)

        assertThrows<ReservationConflictException> {
            service.reserve(ReserveCommand(1L, "r1", amount = 50))
        }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `가용 수량 초과 예약 시 InsufficientStockException`() {
        every { reservationRepository.findById("r1") } returns null
        every { repository.findById(1L) } returns item(quantity = 100, reserved = 80)

        assertThrows<InsufficientStockException> {
            service.reserve(ReserveCommand(1L, "r1", amount = 30))
        }
        verify(exactly = 0) { reservationRepository.save(any()) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `0 이하 수량 예약 시 InvalidAmountException`() {
        every { reservationRepository.findById("r1") } returns null
        every { repository.findById(1L) } returns item(quantity = 100)

        assertThrows<InvalidAmountException> {
            service.reserve(ReserveCommand(1L, "r1", amount = 0))
        }
        verify(exactly = 0) { repository.save(any()) }
    }

    // --- 확정 ---

    @Test
    fun `확정 시 quantity와 reserved 모두 차감`() {
        val item = item(quantity = 100, reserved = 40)
        every { reservationRepository.findById("r1") } returns Reservation("r1", 1L, 40)
        every { repository.findById(1L) } returns item
        every { reservationRepository.save(any()) } returnsArgument 0
        every { repository.save(any()) } returnsArgument 0

        val result = service.confirm(ReservationRef(1L, "r1"))

        assertEquals(60, result.quantity)
        assertEquals(0, result.reserved)
    }

    @Test
    fun `이미 확정된 예약 재확정은 no-op(멱등)`() {
        val item = item(quantity = 60, reserved = 0)
        every { reservationRepository.findById("r1") } returns Reservation("r1", 1L, 40).apply { confirm() }
        every { repository.findById(1L) } returns item

        val result = service.confirm(ReservationRef(1L, "r1"))

        assertEquals(60, result.quantity)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `존재하지 않는 예약 확정 시 ReservationNotFoundException`() {
        every { reservationRepository.findById("nope") } returns null

        assertThrows<ReservationNotFoundException> {
            service.confirm(ReservationRef(1L, "nope"))
        }
    }

    @Test
    fun `취소된 예약을 확정하면 IllegalReservationStateException`() {
        val item = item(quantity = 100, reserved = 0)
        every { reservationRepository.findById("r1") } returns Reservation("r1", 1L, 40).apply { cancel() }
        every { repository.findById(1L) } returns item

        assertThrows<IllegalReservationStateException> {
            service.confirm(ReservationRef(1L, "r1"))
        }
    }

    // --- 취소 ---

    @Test
    fun `취소 시 reserved만 감소하고 quantity 유지`() {
        val item = item(quantity = 100, reserved = 40)
        every { reservationRepository.findById("r1") } returns Reservation("r1", 1L, 40)
        every { repository.findById(1L) } returns item
        every { reservationRepository.save(any()) } returnsArgument 0
        every { repository.save(any()) } returnsArgument 0

        val result = service.cancel(ReservationRef(1L, "r1"))

        assertEquals(100, result.quantity)
        assertEquals(0, result.reserved)
    }

    @Test
    fun `이미 취소된 예약 재취소는 no-op(멱등)`() {
        val item = item(quantity = 100, reserved = 0)
        every { reservationRepository.findById("r1") } returns Reservation("r1", 1L, 40).apply { cancel() }
        every { repository.findById(1L) } returns item

        val result = service.cancel(ReservationRef(1L, "r1"))

        assertEquals(100, result.quantity)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `예약의 재고 id와 경로 id가 다르면 ReservationConflictException`() {
        every { reservationRepository.findById("r1") } returns Reservation("r1", 2L, 40)

        assertThrows<ReservationConflictException> {
            service.confirm(ReservationRef(1L, "r1"))
        }
    }

    private fun item(quantity: Int, reserved: Int = 0) = InventoryItem(
        id = 1L,
        sku = "SKU-001",
        warehouseId = "WH-A",
        quantity = quantity,
        reserved = reserved
    )
}
