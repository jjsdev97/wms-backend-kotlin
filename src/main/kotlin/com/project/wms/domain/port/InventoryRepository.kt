package com.project.wms.domain.port

import com.project.wms.domain.inventory.InventoryItem

interface InventoryRepository {
    fun findById(id: Long): InventoryItem?
    fun findAll(): List<InventoryItem>
    fun save(item: InventoryItem): InventoryItem
}
