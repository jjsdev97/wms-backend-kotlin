package com.project.wms.interfaces.graphql

import com.project.wms.application.inventory.InventoryService
import com.project.wms.interfaces.rest.inventory.InventoryResponse
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

/**
 * GraphQL 인터페이스 — 조회 전용(Query만).
 *
 * 프로토콜별 역할 분담:
 *  - REST  : 외부 시스템의 읽기·쓰기 진입점. 재고 변경(쓰기)은 인증(JWT) 필요.
 *  - GraphQL: 선택적 필드 조회 강점을 살린 "읽기 전용" 인터페이스. 쓰기(Mutation) 없음.
 *  - MCP   : 내부 LLM용. 내부망 가정으로 별도 인증 없이 개방.
 *
 * 과거에는 adjust/reserve/confirm/cancel Mutation도 노출했으나,
 * 쓰기 경로를 REST로 일원화하면서 제거했다. (schema.graphqls의 주석 참고)
 */
@Controller
class InventoryGraphqlController(private val inventoryService: InventoryService) {

    @QueryMapping
    fun inventories(): List<InventoryResponse> =
        inventoryService.getAllInventory().map(InventoryResponse::from)

    @QueryMapping
    fun inventory(@Argument id: Long): InventoryResponse =
        InventoryResponse.from(inventoryService.getInventory(id))
}
