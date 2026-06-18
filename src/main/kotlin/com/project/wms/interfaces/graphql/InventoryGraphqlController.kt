package com.project.wms.interfaces.graphql

import com.project.wms.application.inventory.InventoryService
import com.project.wms.domain.inventory.AdjustStockCommand
import com.project.wms.domain.inventory.ReservationRef
import com.project.wms.domain.inventory.ReserveCommand
import com.project.wms.interfaces.rest.inventory.InventoryResponse
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class InventoryGraphqlController(private val inventoryService: InventoryService) {

    @QueryMapping
    fun inventories(): List<InventoryResponse> =
        inventoryService.getAllInventory().map(InventoryResponse::from)

    @QueryMapping
    fun inventory(@Argument id: Long): InventoryResponse =
        InventoryResponse.from(inventoryService.getInventory(id))

    @MutationMapping
    fun adjustStock(@Argument id: Long, @Argument delta: Int): InventoryResponse =
        InventoryResponse.from(inventoryService.adjustStock(AdjustStockCommand(id, delta)))

    @MutationMapping
    fun reserveStock(
        @Argument id: Long,
        @Argument reservationId: String,
        @Argument amount: Int,
    ): InventoryResponse =
        InventoryResponse.from(inventoryService.reserve(ReserveCommand(id, reservationId, amount)))

    @MutationMapping
    fun confirmStock(@Argument id: Long, @Argument reservationId: String): InventoryResponse =
        InventoryResponse.from(inventoryService.confirm(ReservationRef(id, reservationId)))

    @MutationMapping
    fun cancelReservation(@Argument id: Long, @Argument reservationId: String): InventoryResponse =
        InventoryResponse.from(inventoryService.cancel(ReservationRef(id, reservationId)))
}
