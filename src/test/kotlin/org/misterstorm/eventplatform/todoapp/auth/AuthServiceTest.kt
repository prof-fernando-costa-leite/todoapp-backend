package org.misterstorm.eventplatform.todoapp.auth

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.misterstorm.eventplatform.todoapp.auth.application.port.out.JwtTokenPort
import org.misterstorm.eventplatform.todoapp.auth.application.port.out.UserRepositoryPort
import org.misterstorm.eventplatform.todoapp.auth.application.service.AuthService
import org.misterstorm.eventplatform.todoapp.auth.domain.TokenResponse
import org.misterstorm.eventplatform.todoapp.auth.domain.UserRecord
import org.misterstorm.eventplatform.todoapp.auth.domain.UserResponse
import org.misterstorm.eventplatform.todoapp.auth.entrypoint.http.LoginRequest
import org.misterstorm.eventplatform.todoapp.auth.entrypoint.http.RegisterRequest
import org.misterstorm.eventplatform.todoapp.common.error.domain.ConflictException
import org.misterstorm.eventplatform.todoapp.common.error.domain.UnauthorizedException
import org.misterstorm.eventplatform.todoapp.common.security.model.AuthenticatedUser
import org.misterstorm.eventplatform.todoapp.common.security.service.CurrentUserProvider
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class AuthServiceTest {

    private val userRepository = mockk<UserRepositoryPort>(relaxed = true)
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val jwtService = mockk<JwtTokenPort>()
    private val currentUserProvider = mockk<CurrentUserProvider>()
    private val clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val authService = AuthService(
        userRepository = userRepository,
        passwordEncoder = passwordEncoder,
        jwtTokenPort = jwtService,
        currentUserProvider = currentUserProvider,
        clock = clock,
    )

    @Test
    fun `register should create user when email does not exist`() {
        every { userRepository.findByEmail("alice@example.com") } returns null
        every { passwordEncoder.encode("StrongPass123") } returns "hashed-password"

        val response = authService.register(
            RegisterRequest(
                email = "alice@example.com",
                displayName = "Alice",
                password = "StrongPass123",
            ),
        )

        assertEquals("alice@example.com", response.email)
        assertEquals("Alice", response.displayName)
        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), response.createdAt)
        verify(exactly = 1) { userRepository.insert(any()) }
    }

    @Test
    fun `register should fail when email already exists`() {
        every { userRepository.findByEmail("alice@example.com") } returns
            UserRecord(UUID.randomUUID(), "alice@example.com", "hash", "Alice", Instant.now())

        assertThrows(ConflictException::class.java) {
            authService.register(
                RegisterRequest(
                    email = "alice@example.com",
                    displayName = "Alice",
                    password = "StrongPass123",
                ),
            )
        }
    }

    @Test
    fun `login should return token when credentials are valid`() {
        val user = UserRecord(UUID.randomUUID(), "alice@example.com", "hash", "Alice", Instant.now())
        every { userRepository.findByEmail("alice@example.com") } returns user
        every { passwordEncoder.matches("StrongPass123", "hash") } returns true
        every { jwtService.createToken(user) } returns TokenResponse(
            "token",
            Instant.now(),
            UserResponse(user.id, user.email, user.displayName, user.createdAt),
        )

        val response = authService.login(LoginRequest("alice@example.com", "StrongPass123"))

        assertEquals("token", response.accessToken)
        assertNotNull(response.user)
    }

    @Test
    fun `me should return current authenticated user`() {
        val userId = UUID.randomUUID()
        val user = UserRecord(userId, "alice@example.com", "hash", "Alice", Instant.now())
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "alice@example.com")
        every { userRepository.findById(userId) } returns user

        val response = authService.me()

        assertEquals(userId, response.id)
        assertEquals("alice@example.com", response.email)
    }

    @Test
    fun `listUsers should return id and displayName for all registered users`() {
        val currentUserId = UUID.randomUUID()
        val alice = UserRecord(UUID.randomUUID(), "alice@example.com", "hash", "Alice", Instant.now())
        val bob = UserRecord(UUID.randomUUID(), "bob@example.com", "hash", "Bob", Instant.now())
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(currentUserId, "owner@example.com")
        every { userRepository.listAll() } returns listOf(alice, bob)

        val response = authService.listUsers()

        assertEquals(2, response.size)
        assertEquals(alice.id, response[0].id)
        assertEquals("Alice", response[0].displayName)
        assertEquals(bob.id, response[1].id)
        assertEquals("Bob", response[1].displayName)
    }

    @Test
    fun `login should fail when password is invalid`() {
        val user = UserRecord(UUID.randomUUID(), "alice@example.com", "hash", "Alice", Instant.now())
        every { userRepository.findByEmail("alice@example.com") } returns user
        every { passwordEncoder.matches("wrong", "hash") } returns false

        assertThrows(UnauthorizedException::class.java) {
            authService.login(LoginRequest("alice@example.com", "wrong"))
        }
    }
}
