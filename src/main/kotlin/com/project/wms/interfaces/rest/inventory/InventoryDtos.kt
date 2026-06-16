package com.project.wms.interfaces.rest.inventory

import com.project.wms.domain.inventory.InventoryItem
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class InventoryResponse(
    val id: Long,
    val sku: String,
    val warehouseId: String,
    val quantity: Int,
    val version: Long,
    val updatedAt: Instant
) {
    companion object {
        fun from(item: InventoryItem) = InventoryResponse(
            id = item.id,
            sku = item.sku,
            warehouseId = item.warehouseId,
            quantity = item.quantity,
            version = item.version,
            updatedAt = item.updatedAt
        )
    }
}

data class AdjustStockRequest(
    @field:NotNull(message = "delta는 필수입니다")
    val delta: Int?
)
