package com.project.wms.domain.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "refresh_token")
class RefreshToken(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    val tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) {
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null
        protected set

    val active: Boolean
        get() = revokedAt == null && expiresAt.isAfter(Instant.now())

    fun revoke(now: Instant = Instant.now()) {
        if (revokedAt == null) {
            revokedAt = now
        }
    }
}
