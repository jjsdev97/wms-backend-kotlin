package com.project.wms.infrastructure.persistence

import com.project.wms.domain.inventory.InventoryItem
import com.project.wms.domain.port.InventoryRepository
import org.springframework.stereotype.Repository

@Repository
class InventoryPersistenceAdapter(
    private val jpaRepository: InventoryJpaRepository
) : InventoryRepository {

    override fun findById(id: Long): InventoryItem? =
        jpaRepository.findById(id).orElse(null)

    override fun findAll(): List<InventoryItem> =
        jpaRepository.findAll()

    override fun save(item: InventoryItem): InventoryItem =
        jpaRepository.save(item)
}
