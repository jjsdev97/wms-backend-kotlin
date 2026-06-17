package com.project.wms.interfaces.mcp

import com.project.wms.application.inventory.InventoryService
import com.project.wms.domain.inventory.AdjustStockCommand
import com.project.wms.domain.inventory.InventoryItem
import com.project.wms.domain.inventory.ReservationCommand
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

/**
 * MCP tool 정의. REST·GraphQL과 동일한 [InventoryService]를 재사용한다.
 * Spring AI MCP server starter가 [Tool] 메서드를 MCP tool로 노출한다.
 */
@Service
class InventoryMcpTools(private val inventoryService: InventoryService) {

    @Tool(description = "재고 단건 조회. id로 조회한다.")
    fun getInventory(
        @ToolParam(description = "재고 id") id: Long,
    ): String {
        val item = inventoryService.getInventory(id)
        return describe(item)
    }

    @Tool(description = "재고 증감. delta가 음수면 차감한다.")
    fun adjustStock(
        @ToolParam(description = "재고 id") id: Long,
        @ToolParam(description = "증감량(음수면 차감)") delta: Int,
    ): String =
        "조정 완료: " + describe(inventoryService.adjustStock(AdjustStockCommand(id, delta)))

    @Tool(description = "재고 예약. 가용 수량 내에서 예약분을 늘린다. amount는 양수.")
    fun reserveStock(
        @ToolParam(description = "재고 id") id: Long,
        @ToolParam(description = "예약 수량(양수)") amount: Int,
    ): String =
        "예약 완료: " + describe(inventoryService.reserve(ReservationCommand(id, amount)))

    @Tool(description = "출고 확정. 예약분을 실제 재고에서 차감한다. amount는 양수.")
    fun confirmStock(
        @ToolParam(description = "재고 id") id: Long,
        @ToolParam(description = "확정 수량(양수)") amount: Int,
    ): String =
        "확정 완료: " + describe(inventoryService.confirm(ReservationCommand(id, amount)))

    @Tool(description = "예약 취소. 예약분만 해제하고 총 재고는 유지한다. amount는 양수.")
    fun cancelReservation(
        @ToolParam(description = "재고 id") id: Long,
        @ToolParam(description = "취소 수량(양수)") amount: Int,
    ): String =
        "취소 완료: " + describe(inventoryService.cancel(ReservationCommand(id, amount)))

    private fun describe(item: InventoryItem): String =
        "id=${item.id}, sku=${item.sku}, warehouse=${item.warehouseId}, " +
            "quantity=${item.quantity}, reserved=${item.reserved}, available=${item.available}, version=${item.version}"
}
