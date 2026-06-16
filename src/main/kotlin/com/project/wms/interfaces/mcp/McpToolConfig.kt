package com.project.wms.interfaces.mcp

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class McpToolConfig {

    @Bean
    fun inventoryToolCallbackProvider(tools: InventoryMcpTools): ToolCallbackProvider =
        MethodToolCallbackProvider.builder()
            .toolObjects(tools)
            .build()
}
