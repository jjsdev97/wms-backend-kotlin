package com.project.wms.interfaces.rest.inventory

import com.project.wms.domain.inventory.InventoryItem
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.Instant

data class InventoryResponse(
    val id: Long,
    val sku: String,
    val warehouseId: String,
    val quantity: Int,
    val reserved: Int,
    val available: Int,
    val version: Long,
    val updatedAt: Instant
) {
    companion object {
        fun from(item: InventoryItem) = InventoryResponse(
            id = item.id,
            sku = item.sku,
            warehouseId = item.warehouseId,
            quantity = item.quantity,
            reserved = item.reserved,
            available = item.available,
            version = item.version,
            updatedAt = item.updatedAt
        )
    }
}

data class AdjustStockRequest(
    @field:NotNull(message = "delta는 필수입니다")
    val delta: Int?
)

data class ReserveRequest(
    @field:NotBlank(message = "reservationId는 필수입니다")
    val reservationId: String?,

    @field:NotNull(message = "amount는 필수입니다")
    @field:Positive(message = "amount는 1 이상이어야 합니다")
    val amount: Int?
)

/** 확정·취소 요청. 예약 전체를 대상으로 하므로 reservationId만 받는다. */
data class ReservationRefRequest(
    @field:NotBlank(message = "reservationId는 필수입니다")
    val reservationId: String?
)
