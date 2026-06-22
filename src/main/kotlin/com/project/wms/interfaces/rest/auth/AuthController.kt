package com.project.wms.interfaces.rest.auth

import com.project.wms.application.auth.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
class AuthController(private val authService: AuthService) {

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@Valid @RequestBody request: SignupRequest) {
        authService.register(request.username!!, request.password!!)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): TokenResponse =
        TokenResponse(accessToken = authService.login(request.username!!, request.password!!))
}
