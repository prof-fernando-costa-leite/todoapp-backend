package org.misterstorm.eventplatform.todoapp.auth.entrypoint.http

import org.misterstorm.eventplatform.todoapp.auth.application.service.AuthService
import org.misterstorm.eventplatform.todoapp.auth.domain.TokenResponse
import org.misterstorm.eventplatform.todoapp.auth.domain.UserLookupResponse
import org.misterstorm.eventplatform.todoapp.auth.domain.UserResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): UserResponse = authService.register(request)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): TokenResponse = authService.login(request)

    @GetMapping("/me")
    fun me(): UserResponse = authService.me()

    @GetMapping("/users")
    fun listUsers(): List<UserLookupResponse> = authService.listUsers()
}

