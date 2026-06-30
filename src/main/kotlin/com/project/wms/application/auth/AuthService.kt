package com.project.wms.application.auth

import com.project.wms.domain.port.RefreshTokenRepository
import com.project.wms.domain.port.UserRepository
import com.project.wms.domain.user.InvalidRefreshTokenException
import com.project.wms.domain.user.RefreshToken
import com.project.wms.domain.user.Role
import com.project.wms.domain.user.User
import com.project.wms.domain.user.UsernameAlreadyExistsException
import com.project.wms.infrastructure.security.JwtProperties
import com.project.wms.infrastructure.security.JwtService
import com.project.wms.infrastructure.security.RefreshTokenService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService,
    private val jwtProperties: JwtProperties,
) {

    /** 회원가입. 비밀번호는 BCrypt로 해시해 저장한다. */
    @Transactional
    fun register(username: String, rawPassword: String) {
        if (userRepository.existsByUsername(username)) {
            throw UsernameAlreadyExistsException(username)
        }
        val passwordHash = passwordEncoder.encode(rawPassword)
            ?: error("비밀번호 해시 생성에 실패했습니다.")
        userRepository.save(
            User(
                username = username,
                passwordHash = passwordHash,
                role = Role.USER,
            )
        )
    }

    /**
     * 로그인. AuthenticationManager가 자격증명을 검증(실패 시 BadCredentialsException)하고,
     * 성공하면 JWT access token을 발급한다.
     */
    @Transactional
    fun login(username: String, rawPassword: String): AuthTokens {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(username, rawPassword)
        )
        val user = userRepository.findByUsername(username)
            ?: error("인증된 사용자를 찾을 수 없습니다.")
        return issueTokens(user, accessToken = jwtService.issueToken(authentication))
    }

    @Transactional
    fun refresh(refreshToken: String): AuthTokens {
        val stored = refreshTokenRepository.findByTokenHash(refreshTokenService.hash(refreshToken))
            ?: throw InvalidRefreshTokenException()
        if (!stored.active) {
            throw InvalidRefreshTokenException()
        }

        val user = userRepository.findById(stored.userId)
            ?: throw InvalidRefreshTokenException()
        stored.revoke()
        refreshTokenRepository.save(stored)

        return issueTokens(user, accessToken = jwtService.issueToken(user.username, user.role))
    }

    @Transactional
    fun logout(refreshToken: String) {
        refreshTokenRepository.findByTokenHash(refreshTokenService.hash(refreshToken))
            ?.let {
                it.revoke()
                refreshTokenRepository.save(it)
            }
    }

    @Transactional
    fun logoutAll(username: String) {
        val user = userRepository.findByUsername(username) ?: return
        refreshTokenRepository.revokeAllByUserId(user.id)
    }

    private fun issueTokens(user: User, accessToken: String): AuthTokens {
        val refreshToken = refreshTokenService.generate()
        refreshTokenRepository.save(
            RefreshToken(
                userId = user.id,
                tokenHash = refreshTokenService.hash(refreshToken),
                expiresAt = Instant.now().plus(jwtProperties.refreshExpirationDays, ChronoUnit.DAYS),
            )
        )
        return AuthTokens(accessToken = accessToken, refreshToken = refreshToken)
    }
}

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)
