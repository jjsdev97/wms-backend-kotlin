package com.project.wms.infrastructure.persistence

import com.project.wms.domain.port.UserRepository
import com.project.wms.domain.user.User
import org.springframework.stereotype.Repository

@Repository
class UserPersistenceAdapter(
    private val jpaRepository: UserJpaRepository
) : UserRepository {

    override fun findById(id: Long): User? =
        jpaRepository.findById(id).orElse(null)

    override fun findByUsername(username: String): User? =
        jpaRepository.findByUsername(username)

    override fun existsByUsername(username: String): Boolean =
        jpaRepository.existsByUsername(username)

    override fun save(user: User): User =
        jpaRepository.save(user)
}
