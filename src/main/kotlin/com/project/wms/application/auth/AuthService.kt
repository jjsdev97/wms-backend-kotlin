package com.project.wms.application.auth

import com.project.wms.domain.port.UserRepository
import com.project.wms.domain.user.Role
import com.project.wms.domain.user.User
import com.project.wms.domain.user.UsernameAlreadyExistsException
import com.project.wms.infrastructure.security.JwtService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService,
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
    fun login(username: String, rawPassword: String): String {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(username, rawPassword)
        )
        return jwtService.issueToken(authentication)
    }
}
