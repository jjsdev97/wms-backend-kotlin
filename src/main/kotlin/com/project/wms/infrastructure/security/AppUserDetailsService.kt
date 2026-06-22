package com.project.wms.infrastructure.security

import com.project.wms.domain.port.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * DB의 app_user를 Spring Security UserDetails로 변환한다.
 * DaoAuthenticationProvider가 로그인 시 username으로 조회 → 저장된 BCrypt 해시와 비교한다.
 */
@Service
class AppUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("사용자를 찾을 수 없습니다: $username")
        return SpringUser.withUsername(user.username)
            .password(user.passwordHash)
            .authorities(SimpleGrantedAuthority("ROLE_${user.role.name}"))
            .build()
    }
}
