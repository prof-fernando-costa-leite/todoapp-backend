package org.misterstorm.eventplatform.todoapp.auth.application.service

import org.misterstorm.eventplatform.todoapp.auth.application.port.out.JwtTokenPort
import org.misterstorm.eventplatform.todoapp.auth.application.port.out.UserRepositoryPort
import org.misterstorm.eventplatform.todoapp.auth.domain.UserRecord
import org.misterstorm.eventplatform.todoapp.auth.domain.UserLookupResponse
import org.misterstorm.eventplatform.todoapp.auth.domain.UserResponse
import org.misterstorm.eventplatform.todoapp.auth.domain.toLookupResponse
import org.misterstorm.eventplatform.todoapp.auth.domain.toResponse
import org.misterstorm.eventplatform.todoapp.auth.entrypoint.http.LoginRequest
import org.misterstorm.eventplatform.todoapp.auth.entrypoint.http.RegisterRequest
import org.misterstorm.eventplatform.todoapp.common.error.domain.ConflictException
import org.misterstorm.eventplatform.todoapp.common.error.domain.UnauthorizedException
import org.misterstorm.eventplatform.todoapp.common.security.service.CurrentUserProvider
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
@Validated
class AuthService(
    private val userRepository: UserRepositoryPort,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenPort: JwtTokenPort,
    private val currentUserProvider: CurrentUserProvider,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun register(request: RegisterRequest): UserResponse {
        val normalizedEmail = request.email.trim().lowercase()
        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw ConflictException("Ja existe um usuario com este e-mail")
        }

        val user = UserRecord(
            id = UUID.randomUUID(),
            email = normalizedEmail,
            passwordHash = requireNotNull(passwordEncoder.encode(request.password)) {
                "Falha ao gerar hash de senha"
            },
            displayName = request.displayName.trim(),
            createdAt = Instant.now(clock),
        )
        userRepository.insert(user)
        logger.info("Usuario cadastrado com sucesso")
        return user.toResponse()
    }

    fun login(request: LoginRequest) =
        userRepository.findByEmail(request.email.trim().lowercase())
            ?.takeIf { passwordEncoder.matches(request.password, it.passwordHash) }
            ?.let {
                logger.info("Usuario autenticado com sucesso")
                jwtTokenPort.createToken(it)
            }
            ?: throw UnauthorizedException("Credenciais invalidas")

    fun me(): UserResponse {
        val currentUser = currentUserProvider.requireCurrentUser()
        val user = userRepository.findById(currentUser.userId)
            ?: throw UnauthorizedException("Usuario autenticado nao encontrado")
        return user.toResponse()
    }

    fun listUsers(): List<UserLookupResponse> {
        currentUserProvider.requireCurrentUser()
        return userRepository.listAll().map { it.toLookupResponse() }
    }
}

