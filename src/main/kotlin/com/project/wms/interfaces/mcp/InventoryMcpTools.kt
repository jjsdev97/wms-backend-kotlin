package com.project.wms.interfaces.mcp

import com.project.wms.application.inventory.InventoryService
import com.project.wms.domain.inventory.AdjustStockCommand
import com.project.wms.domain.inventory.InventoryItem
import com.project.wms.domain.inventory.ReservationRef
import com.project.wms.domain.inventory.ReserveCommand
import com.project.wms.infrastructure.idempotency.IdempotencyExecutor
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

/**
 * MCP tool 정의. REST·GraphQL과 동일한 [InventoryService]를 재사용한다.
 * Spring AI MCP server starter가 [Tool] 메서드를 MCP tool로 노출한다.
 *
 * 예약·확정·취소는 `reservationId` 자연키로 도메인 차원에서 멱등하므로, LLM이
 * 같은 키로 재호출해도 중복 차감이 없다. 반면 [adjustStock]은 자연키가 없는
 * 순수 증감이라, 선택적 [requestId]로 [IdempotencyExecutor] 멱등 처리를 건다.
 */
@Service
class InventoryMcpTools(
    private val inventoryService: InventoryService,
    private val idempotency: IdempotencyExecutor,
) {

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
        @ToolParam(
            required = false,
            description = "멱등 키(선택). 같은 키로 재호출하면 중복 증감 없이 첫 결과를 반환한다.",
        ) requestId: String? = null,
    ): String =
        idempotency.execute(requestId, "mcp:adjustStock", String::class.java, IdempotencyExecutor.DEFAULT_TTL_SECONDS) {
            "조정 완료: " + describe(inventoryService.adjustStock(AdjustStockCommand(id, delta)))
        }

    @Tool(description = "재고 예약. 가용 수량 내에서 예약분을 늘린다. 같은 reservationId 재호출은 중복 예약되지 않는다.")
    fun reserveStock(
        @ToolParam(description = "재고 id") id: Long,
        @ToolParam(description = "예약 식별 자연키. 같은 키면 한 번만 예약된다.") reservationId: String,
        @ToolParam(description = "예약 수량(양수)") amount: Int,
    ): String =
        "예약 완료: " + describe(inventoryService.reserve(ReserveCommand(id, reservationId, amount)))

    @Tool(description = "출고 확정. reservationId가 가리키는 예약 전체를 실제 재고에서 차감한다. 이미 확정된 예약이면 변화 없음.")
    fun confirmStock(
        @ToolParam(description = "재고 id") id: Long,
        @ToolParam(description = "확정할 예약 식별 자연키") reservationId: String,
    ): String =
        "확정 완료: " + describe(inventoryService.confirm(ReservationRef(id, reservationId)))

    @Tool(description = "예약 취소. reservationId가 가리키는 예약 전체를 해제한다. 이미 취소된 예약이면 변화 없음.")
    fun cancelReservation(
        @ToolParam(description = "재고 id") id: Long,
        @ToolParam(description = "취소할 예약 식별 자연키") reservationId: String,
    ): String =
        "취소 완료: " + describe(inventoryService.cancel(ReservationRef(id, reservationId)))

    private fun describe(item: InventoryItem): String =
        "id=${item.id}, sku=${item.sku}, warehouse=${item.warehouseId}, " +
            "quantity=${item.quantity}, reserved=${item.reserved}, available=${item.available}, version=${item.version}"
}
