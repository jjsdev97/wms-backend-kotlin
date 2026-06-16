package com.project.wms.interfaces.rest.inventory

import com.project.wms.application.inventory.InventoryService
import com.project.wms.domain.inventory.AdjustStockCommand
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
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

    @PostMapping("/{id}/adjust")
    fun adjust(
        @PathVariable id: Long,
        @Valid @RequestBody request: AdjustStockRequest
    ): InventoryResponse =
        InventoryResponse.from(
            inventoryService.adjustStock(AdjustStockCommand(id, request.delta!!))
        )
}
