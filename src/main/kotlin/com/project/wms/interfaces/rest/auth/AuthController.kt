package com.project.wms.interfaces.rest.auth

import com.project.wms.application.auth.AuthService
import com.project.wms.infrastructure.security.AuthRateLimiter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인증 진입점. 회원가입·로그인은 토큰을 받기 전 단계라 SecurityConfig에서 공개(permitAll)된다.
 * 발급된 accessToken은 보호 API 요청에 `Authorization: Bearer <token>`으로 실어 보내고,
 * refreshToken은 `/refresh`에서 새 토큰 쌍으로 회전한다.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val rateLimiter: AuthRateLimiter,
) {

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(
        servletRequest: HttpServletRequest,
        @Valid @RequestBody request: SignupRequest,
    ) {
        rateLimiter.check(rateLimitKey(servletRequest, "signup", request.username))
        authService.register(request.username!!, request.password!!)
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "access token과 refresh token을 발급한다.")
    fun login(
        servletRequest: HttpServletRequest,
        @Valid @RequestBody request: LoginRequest,
    ): TokenResponse {
        rateLimiter.check(rateLimitKey(servletRequest, "login", request.username))
        return authService.login(request.username!!, request.password!!).toResponse()
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "refresh token을 검증하고 새 access token과 refresh token을 발급한다. 기존 refresh token은 폐기된다.")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): TokenResponse =
        authService.refresh(request.refreshToken!!).toResponse()

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "전달한 refresh token을 폐기한다.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody request: RefreshTokenRequest) {
        authService.logout(request.refreshToken!!)
    }

    @PostMapping("/logout-all")
    @Operation(summary = "전체 로그아웃", description = "현재 access token 사용자에게 발급된 모든 refresh token을 폐기한다.")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logoutAll(authentication: Authentication) {
        authService.logoutAll(authentication.name)
    }

    private fun rateLimitKey(request: HttpServletRequest, action: String, username: String?): String {
        val client = request.getHeader("X-Forwarded-For")
            ?.substringBefore(",")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: request.remoteAddr
        return "$action:$client:${username.orEmpty().lowercase()}"
    }

    private fun com.project.wms.application.auth.AuthTokens.toResponse(): TokenResponse =
        TokenResponse(accessToken = accessToken, refreshToken = refreshToken)
}
