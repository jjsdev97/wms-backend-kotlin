package com.project.wms.infrastructure.security

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import com.project.wms.domain.user.Role
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * 인증 성공 후 JWT(access token)를 발급한다. HS256 대칭키 서명(JwtEncoder 빈은 SecurityConfig 참고).
 *
 * 토큰은 자가검증(stateless)이라 만료 전 무효화는 불가하다. 학습 범위에서는 짧은 만료로 감수한다.
 * 즉시 무효화가 필요하면 opaque 토큰 + Redis 저장 방식으로 전환해야 한다.
 */
@Service
class JwtService(
    private val jwtEncoder: JwtEncoder,
    private val properties: JwtProperties,
) {
    fun issueToken(authentication: Authentication): String {
        val scope = authentication.authorities.joinToString(" ") { it.authority ?: "" }
        return issueToken(authentication.name, scope)
    }

    fun issueToken(username: String, role: Role): String =
        issueToken(username, "ROLE_${role.name}")

    private fun issueToken(username: String, scope: String): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer("k-wms")
            .issuedAt(now)
            .expiresAt(now.plus(properties.expirationMinutes, ChronoUnit.MINUTES))
            .subject(username)
            .claim("scope", scope)
            .build()
        // 대칭키(HS256) 사용 시 서명 알고리즘을 헤더로 명시해야 Nimbus가 서명키를 선택할 수 있다.
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }
}
