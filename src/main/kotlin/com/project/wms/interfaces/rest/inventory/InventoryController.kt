package com.project.wms.interfaces.rest.inventory

import com.project.wms.application.inventory.InventoryService
import com.project.wms.domain.inventory.AdjustStockCommand
import com.project.wms.domain.inventory.ReservationRef
import com.project.wms.domain.inventory.ReserveCommand
import com.project.wms.infrastructure.idempotency.Idempotent
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(private val inventoryService: InventoryService) {

    @GetMapping
    fun getAll(): List<InventoryResponse> =
        inventoryService.getAllInventory().map(InventoryResponse::from)

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Long): InventoryResponse =
        InventoryResponse.from(inventoryService.getInventory(id))

    @Idempotent
    @PostMapping("/{id}/adjust")
    @Operation(summary = "재고 증감", description = "Idempotency-Key 헤더를 보내면 같은 요청의 중복 증감을 방지한다.")
    @SecurityRequirement(name = "bearerAuth")
    fun adjust(
        @PathVariable id: Long,
        @Parameter(
            name = "Idempotency-Key",
            description = "선택 멱등 키. 같은 키와 같은 요청 본문은 최초 응답을 재사용하고, 같은 키의 다른 요청은 409로 거절된다.",
            `in` = ParameterIn.HEADER,
            required = false,
        )
        @RequestHeader("Idempotency-Key", required = false)
        idempotencyKey: String?,
        @Valid @RequestBody request: AdjustStockRequest
    ): InventoryResponse =
        InventoryResponse.from(
            inventoryService.adjustStock(AdjustStockCommand(id, request.delta!!))
        )

    @PostMapping("/{id}/reserve")
    @SecurityRequirement(name = "bearerAuth")
    fun reserve(
        @PathVariable id: Long,
        @Valid @RequestBody request: ReserveRequest
    ): InventoryResponse =
        InventoryResponse.from(
            inventoryService.reserve(ReserveCommand(id, request.reservationId!!, request.amount!!))
        )

    @PostMapping("/{id}/confirm")
    @SecurityRequirement(name = "bearerAuth")
    fun confirm(
        @PathVariable id: Long,
        @Valid @RequestBody request: ReservationRefRequest
    ): InventoryResponse =
        InventoryResponse.from(inventoryService.confirm(ReservationRef(id, request.reservationId!!)))

    @PostMapping("/{id}/cancel")
    @SecurityRequirement(name = "bearerAuth")
    fun cancel(
        @PathVariable id: Long,
        @Valid @RequestBody request: ReservationRefRequest
    ): InventoryResponse =
        InventoryResponse.from(inventoryService.cancel(ReservationRef(id, request.reservationId!!)))
}
