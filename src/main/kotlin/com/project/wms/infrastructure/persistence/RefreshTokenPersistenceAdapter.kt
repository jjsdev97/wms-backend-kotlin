package com.project.wms.infrastructure.persistence

import com.project.wms.domain.port.RefreshTokenRepository
import com.project.wms.domain.user.RefreshToken
import org.springframework.stereotype.Repository

@Repository
class RefreshTokenPersistenceAdapter(
    private val jpaRepository: RefreshTokenJpaRepository,
) : RefreshTokenRepository {

    override fun findByTokenHash(tokenHash: String): RefreshToken? =
        jpaRepository.findByTokenHash(tokenHash)

    override fun save(token: RefreshToken): RefreshToken =
        jpaRepository.save(token)

    override fun revokeAllByUserId(userId: Long) {
        jpaRepository.revokeAllByUserId(userId)
    }
}
