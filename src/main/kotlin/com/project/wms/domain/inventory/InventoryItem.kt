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

    quantity: Int,

    reserved: Int = 0
) {
    @Column(nullable = false)
    var quantity: Int = quantity
        protected set

    @Column(nullable = false)
    var reserved: Int = reserved
        protected set

    @Version
    var version: Long = 0
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    /** 예약 가능한 수량. 총 재고에서 예약분을 뺀 값. */
    val available: Int
        @Transient get() = quantity - reserved

    /** 총 재고 증감. 차감 시 이미 예약된 수량 아래로는 내릴 수 없다. */
    fun adjust(delta: Int) {
        val newQuantity = quantity + delta
        if (newQuantity < reserved) throw InsufficientStockException(id, available, delta)
        quantity = newQuantity
        touch()
    }

    /** 예약. 가용 수량 내에서 예약분을 늘린다. */
    fun reserve(amount: Int) {
        requirePositive(amount)
        if (available < amount) throw InsufficientStockException(id, available, amount)
        reserved += amount
        touch()
    }

    /** 출고 확정. 예약분을 실제 재고에서 차감한다. */
    fun confirm(amount: Int) {
        requirePositive(amount)
        if (reserved < amount) throw InsufficientReservationException(id, reserved, amount)
        quantity -= amount
        reserved -= amount
        touch()
    }

    /** 예약 취소. 예약분만 줄이고 총 재고는 유지한다. */
    fun cancel(amount: Int) {
        requirePositive(amount)
        if (reserved < amount) throw InsufficientReservationException(id, reserved, amount)
        reserved -= amount
        touch()
    }

    private fun requirePositive(amount: Int) {
        if (amount <= 0) throw InvalidAmountException(amount)
    }

    private fun touch() {
        updatedAt = Instant.now()
    }
}
