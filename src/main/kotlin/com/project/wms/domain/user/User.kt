package com.project.wms.domain.user

import jakarta.persistence.*
import java.time.Instant

/** 인증용 사용자 계정. password는 평문이 아닌 BCrypt 해시(passwordHash)로만 보관한다. */
@Entity
@Table(name = "app_user")
class User(
    @Column(nullable = false, unique = true, length = 50)
    val username: String,

    @Column(name = "password_hash", nullable = false, length = 100)
    val passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: Role = Role.USER,

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) {
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
        protected set
}
