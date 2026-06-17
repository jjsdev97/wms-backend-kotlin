package com.project.wms.domain.inventory

data class AdjustStockCommand(
    val inventoryId: Long,
    val delta: Int
)

/** 예약·확정·취소 공통 명령. amount는 양수. */
data class ReservationCommand(
    val inventoryId: Long,
    val amount: Int
)
