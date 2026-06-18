package com.project.wms.application.inventory

import com.project.wms.domain.inventory.AdjustStockCommand
import com.project.wms.domain.inventory.InventoryItem
import com.project.wms.domain.inventory.InventoryNotFoundException
import com.project.wms.domain.inventory.Reservation
import com.project.wms.domain.inventory.ReservationConflictException
import com.project.wms.domain.inventory.ReservationNotFoundException
import com.project.wms.domain.inventory.ReservationRef
import com.project.wms.domain.inventory.ReservationStatus
import com.project.wms.domain.inventory.ReserveCommand
import com.project.wms.domain.port.InventoryRepository
import com.project.wms.domain.port.ReservationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: ReservationRepository,
) {

    fun getInventory(id: Long): InventoryItem =
        inventoryRepository.findById(id) ?: throw InventoryNotFoundException(id)

    fun getAllInventory(): List<InventoryItem> =
        inventoryRepository.findAll()

    @Transactional
    fun adjustStock(command: AdjustStockCommand): InventoryItem {
        val item = inventoryRepository.findById(command.inventoryId)
            ?: throw InventoryNotFoundException(command.inventoryId)
        item.adjust(command.delta)
        return inventoryRepository.save(item)
    }

    /**
     * 예약 생성. reservationId 자연키로 멱등하다 — 같은 키의 재호출은 재고를
     * 다시 차감하지 않고 현재 상태를 반환한다. 같은 키에 다른 재고/수량이면 충돌.
     */
    @Transactional
    fun reserve(command: ReserveCommand): InventoryItem {
        reservationRepository.findById(command.reservationId)?.let { existing ->
            if (existing.inventoryId != command.inventoryId || existing.amount != command.amount) {
                throw ReservationConflictException(command.reservationId)
            }
            return loadInventory(command.inventoryId)
        }

        val item = loadInventory(command.inventoryId)
        item.reserve(command.amount)
        reservationRepository.save(
            Reservation(command.reservationId, command.inventoryId, command.amount)
        )
        return inventoryRepository.save(item)
    }

    /** 출고 확정. 이미 확정된 예약이면 no-op으로 현재 재고를 반환(멱등). */
    @Transactional
    fun confirm(ref: ReservationRef): InventoryItem {
        val reservation = loadReservation(ref)
        if (reservation.status == ReservationStatus.CONFIRMED) {
            return loadInventory(reservation.inventoryId)
        }
        val item = loadInventory(reservation.inventoryId)
        reservation.confirm()
        item.confirm(reservation.amount)
        reservationRepository.save(reservation)
        return inventoryRepository.save(item)
    }

    /** 예약 취소. 이미 취소된 예약이면 no-op으로 현재 재고를 반환(멱등). */
    @Transactional
    fun cancel(ref: ReservationRef): InventoryItem {
        val reservation = loadReservation(ref)
        if (reservation.status == ReservationStatus.CANCELLED) {
            return loadInventory(reservation.inventoryId)
        }
        val item = loadInventory(reservation.inventoryId)
        reservation.cancel()
        item.cancel(reservation.amount)
        reservationRepository.save(reservation)
        return inventoryRepository.save(item)
    }

    private fun loadInventory(id: Long): InventoryItem =
        inventoryRepository.findById(id) ?: throw InventoryNotFoundException(id)

    private fun loadReservation(ref: ReservationRef): Reservation {
        val reservation = reservationRepository.findById(ref.reservationId)
            ?: throw ReservationNotFoundException(ref.reservationId)
        if (reservation.inventoryId != ref.inventoryId) {
            throw ReservationConflictException(ref.reservationId)
        }
        return reservation
    }
}
