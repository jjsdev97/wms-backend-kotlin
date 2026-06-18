package com.project.wms.domain.inventory

data class AdjustStockCommand(
    val inventoryId: Long,
    val delta: Int
)

/** 예약 생성. reservationId는 클라이언트가 제공하는 멱등 자연키. */
data class ReserveCommand(
    val inventoryId: Long,
    val reservationId: String,
    val amount: Int
)

/** 예약 확정·취소 참조. 예약 전체를 대상으로 하므로 amount는 받지 않는다. */
data class ReservationRef(
    val inventoryId: Long,
    val reservationId: String
)
