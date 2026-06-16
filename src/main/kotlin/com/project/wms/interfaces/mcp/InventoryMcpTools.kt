package com.project.wms.interfaces.mcp

import com.project.wms.application.inventory.InventoryService
import com.project.wms.domain.inventory.AdjustStockCommand
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
        return "id=${item.id}, sku=${item.sku}, warehouse=${item.warehouseId}, quantity=${item.quantity}"
    }

    @Tool(description = "재고 증감. delta가 음수면 차감한다.")
    fun adjustStock(
        @ToolParam(description = "재고 id") id: Long,
        @ToolParam(description = "증감량(음수면 차감)") delta: Int,
    ): String {
        val item = inventoryService.adjustStock(AdjustStockCommand(id, delta))
        return "조정 완료: id=${item.id}, quantity=${item.quantity}, version=${item.version}"
    }
}
