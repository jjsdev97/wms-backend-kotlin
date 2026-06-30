package com.project.wms.interfaces.rest

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun wmsOpenAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("WMS API")
                    .description("창고 관리 시스템 REST API. 로그인은 access token과 refresh token을 발급하며, 보호 API는 Bearer access token을 사용한다.")
                    .version("v0.0.1")
            )
            .components(
                Components().addSecuritySchemes(
                    "bearerAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )
}
