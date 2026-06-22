package com.project.wms.interfaces.rest.auth

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignupRequest(
    @field:NotBlank
    @field:Size(min = 3, max = 50)
    val username: String?,

    @field:NotBlank
    // 문자 수 상한. BCrypt는 입력 앞 72바이트만 반영하므로, ASCII 기준 72자로 맞춰 조용한 절단을 피한다.
    // (멀티바이트 문자면 72자가 72바이트를 넘을 수 있으나, 길이 제한 목적상 보수적으로 72자 유지)
    @field:Size(min = 8, max = 72)
    val password: String?,
)

data class LoginRequest(
    @field:NotBlank
    val username: String?,

    @field:NotBlank
    val password: String?,
)

data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
)
