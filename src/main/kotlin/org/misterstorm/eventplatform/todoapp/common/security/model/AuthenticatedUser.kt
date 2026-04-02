package org.misterstorm.eventplatform.todoapp.common.security.model

import java.util.UUID

data class AuthenticatedUser(
    val userId: UUID,
    val email: String,
)

