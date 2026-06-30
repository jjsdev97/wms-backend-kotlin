package com.project.wms.interfaces.rest.auth

import com.project.wms.application.auth.AuthService
import com.project.wms.infrastructure.security.AuthRateLimiter
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
 * 발급된 accessToken을 이후 재고 쓰기 요청에 `Authorization: Bearer <token>`으로 실어 보낸다.
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
    fun login(
        servletRequest: HttpServletRequest,
        @Valid @RequestBody request: LoginRequest,
    ): TokenResponse {
        rateLimiter.check(rateLimitKey(servletRequest, "login", request.username))
        return authService.login(request.username!!, request.password!!).toResponse()
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): TokenResponse =
        authService.refresh(request.refreshToken!!).toResponse()

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody request: RefreshTokenRequest) {
        authService.logout(request.refreshToken!!)
    }

    @PostMapping("/logout-all")
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
