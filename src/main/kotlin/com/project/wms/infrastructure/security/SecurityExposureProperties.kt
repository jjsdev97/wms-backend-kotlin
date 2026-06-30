package com.project.wms.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security.exposure")
data class SecurityExposureProperties(
    val mcpPublic: Boolean = false,
    val devEndpointsPublic: Boolean = false,
)
