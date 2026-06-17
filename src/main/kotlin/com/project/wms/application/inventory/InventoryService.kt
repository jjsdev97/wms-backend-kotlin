package com.project.wms.application.inventory

import com.project.wms.domain.inventory.AdjustStockCommand
import com.project.wms.domain.inventory.InventoryItem
import com.project.wms.domain.inventory.InventoryNotFoundException
import com.project.wms.domain.inventory.ReservationCommand
import com.project.wms.domain.port.InventoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InventoryService(private val inventoryRepository: InventoryRepository) {

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

    @Transactional
    fun reserve(command: ReservationCommand): InventoryItem =
        mutate(command.inventoryId) { it.reserve(command.amount) }

    @Transactional
    fun confirm(command: ReservationCommand): InventoryItem =
        mutate(command.inventoryId) { it.confirm(command.amount) }

    @Transactional
    fun cancel(command: ReservationCommand): InventoryItem =
        mutate(command.inventoryId) { it.cancel(command.amount) }

    private inline fun mutate(id: Long, block: (InventoryItem) -> Unit): InventoryItem {
        val item = inventoryRepository.findById(id) ?: throw InventoryNotFoundException(id)
        block(item)
        return inventoryRepository.save(item)
    }
}
