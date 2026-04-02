package org.misterstorm.eventplatform.todoapp.auth.entrypoint.http

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:Email(message = "Please provide a valid email address")
    @field:NotBlank(message = "Email is required")
    val email: String,
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 3, max = 120, message = "Name must be between 3 and 120 characters")
    val displayName: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 120, message = "Password must be between 8 and 120 characters")
    val password: String,
)

data class LoginRequest(
    @field:Email(message = "Please provide a valid email address")
    @field:NotBlank(message = "Email is required")
    val email: String,
    @field:NotBlank(message = "Password is required")
    val password: String,
)

