package com.project.wms.interfaces.rest.auth

import com.project.wms.interfaces.rest.validation.Utf8ByteSize
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignupRequest(
    @field:NotBlank
    @field:Size(min = 3, max = 50)
    val username: String?,

    @field:NotBlank
    @field:Utf8ByteSize(min = 8, max = 72, message = "password는 UTF-8 기준 8~72바이트여야 합니다")
    val password: String?,
)

data class LoginRequest(
    @field:NotBlank
    val username: String?,

    @field:NotBlank
    @field:Utf8ByteSize(max = 72, message = "password는 UTF-8 기준 72바이트 이하여야 합니다")
    val password: String?,
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
)

data class RefreshTokenRequest(
    @field:NotBlank
    val refreshToken: String?,
)
