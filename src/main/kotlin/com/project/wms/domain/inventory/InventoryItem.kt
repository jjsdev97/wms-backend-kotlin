package com.project.wms.domain.inventory

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "inventory_item",
    uniqueConstraints = [UniqueConstraint(columnNames = ["sku", "warehouse_id"])]
)
class InventoryItem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100)
    val sku: String,

    @Column(name = "warehouse_id", nullable = false, length = 50)
    val warehouseId: String,

    @Column(nullable = false)
    var quantity: Int,

    @Version
    var version: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    fun adjust(delta: Int) {
        val newQuantity = quantity + delta
        if (newQuantity < 0) throw InsufficientStockException(id, quantity, delta)
        quantity = newQuantity
        updatedAt = Instant.now()
    }
}
