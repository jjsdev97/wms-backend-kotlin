package com.project.wms.infrastructure.persistence

import com.project.wms.domain.inventory.InventoryItem
import org.springframework.data.jpa.repository.JpaRepository

interface InventoryJpaRepository : JpaRepository<InventoryItem, Long>
