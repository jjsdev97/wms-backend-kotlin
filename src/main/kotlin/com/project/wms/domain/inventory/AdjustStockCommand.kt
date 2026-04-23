package com.project.wms.domain.inventory

data class AdjustStockCommand(
    val inventoryId: Long,
    val delta: Int
)
