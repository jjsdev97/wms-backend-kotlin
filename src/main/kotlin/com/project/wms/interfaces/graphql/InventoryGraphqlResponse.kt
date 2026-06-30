package com.project.wms.interfaces.graphql

import com.project.wms.domain.inventory.InventoryItem
import java.time.Instant

data class InventoryGraphqlResponse(
    val id: Long,
    val sku: String,
    val warehouseId: String,
    val quantity: Int,
    val reserved: Int,
    val available: Int,
    val version: String,
    val updatedAt: Instant,
) {
    companion object {
        fun from(item: InventoryItem) = InventoryGraphqlResponse(
            id = item.id,
            sku = item.sku,
            warehouseId = item.warehouseId,
            quantity = item.quantity,
            reserved = item.reserved,
            available = item.available,
            version = item.version.toString(),
            updatedAt = item.updatedAt,
        )
    }
}
