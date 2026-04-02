package org.misterstorm.eventplatform.todoapp.auth.application.port.out

import org.misterstorm.eventplatform.todoapp.auth.domain.UserRecord
import java.util.UUID

interface UserRepositoryPort {
    fun findByEmail(email: String): UserRecord?

    fun findById(id: UUID): UserRecord?

    fun existsById(id: UUID): Boolean

    fun listAll(): List<UserRecord>

    fun insert(user: UserRecord)
}

