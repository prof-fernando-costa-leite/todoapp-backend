package org.misterstorm.eventplatform.todoapp.common.security.service

import org.misterstorm.eventplatform.todoapp.common.error.domain.UnauthorizedException
import org.misterstorm.eventplatform.todoapp.common.security.model.AuthenticatedUser
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class CurrentUserProvider {

    fun requireCurrentUser(): AuthenticatedUser {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw UnauthorizedException("Usuario nao autenticado")
        val principal = authentication.principal as? AuthenticatedUser
            ?: throw UnauthorizedException("Usuario nao autenticado")
        return principal
    }
}
