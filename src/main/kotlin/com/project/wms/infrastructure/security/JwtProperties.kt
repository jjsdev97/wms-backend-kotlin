package com.project.wms.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * JWT 설정. secret은 HS256용 대칭키로 최소 256bit(32바이트) 이상이어야 한다.
 * 운영에서는 환경변수(APP_JWT_SECRET)로 주입하고 소스에 커밋하지 않는다.
 */
@ConfigurationProperties(prefix = "app.jwt")
data class JwtProperties(
    val secret: String,
    val expirationMinutes: Long = 15,
    val refreshExpirationDays: Long = 14,
) {
    init {
        // HS256은 키 길이가 해시 출력(256bit=32byte) 이상이어야 한다. 짧으면 기동 시점에 막는다.
        require(secret.toByteArray().size >= 32) {
            "app.jwt.secret must be at least 32 bytes for HS256 (was ${secret.toByteArray().size})"
        }
    }
}
