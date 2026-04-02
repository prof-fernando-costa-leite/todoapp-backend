package org.misterstorm.eventplatform.todoapp.auth.application.port.out

import org.misterstorm.eventplatform.todoapp.auth.domain.TokenResponse
import org.misterstorm.eventplatform.todoapp.auth.domain.UserRecord
import org.misterstorm.eventplatform.todoapp.common.security.model.AuthenticatedUser

interface JwtTokenPort {
    fun createToken(user: UserRecord): TokenResponse

    fun parse(token: String): AuthenticatedUser
}

