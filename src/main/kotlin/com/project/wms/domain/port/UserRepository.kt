package com.project.wms.domain.port

import com.project.wms.domain.user.User

interface UserRepository {
    fun findById(id: Long): User?
    fun findByUsername(username: String): User?
    fun existsByUsername(username: String): Boolean
    fun save(user: User): User
}
