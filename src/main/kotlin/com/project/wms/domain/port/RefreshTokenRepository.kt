package com.project.wms.domain.port

import com.project.wms.domain.user.RefreshToken

interface RefreshTokenRepository {
    fun findByTokenHash(tokenHash: String): RefreshToken?
    fun save(token: RefreshToken): RefreshToken
    fun revokeAllByUserId(userId: Long)
}
