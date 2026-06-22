package com.project.wms.interfaces.rest

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
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
                    .description("창고 관리 시스템 REST API")
                    .version("v0.0.1")
            )
            // 재고 쓰기 엔드포인트는 JWT Bearer 인증이 필요하다.
            // Swagger UI의 Authorize 버튼에 로그인으로 받은 accessToken을 넣으면 호출에 헤더가 실린다.
            // (조회·인증 엔드포인트는 인증이 필요 없지만, 전역 표시해도 호출엔 지장 없다)
            .components(
                Components().addSecuritySchemes(
                    "bearerAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
}
