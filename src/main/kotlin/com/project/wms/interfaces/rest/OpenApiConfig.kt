package com.project.wms.interfaces.rest

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun wmsOpenAPI(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("WMS API")
                .description("창고 관리 시스템 REST API")
                .version("v0.0.1")
        )
}
