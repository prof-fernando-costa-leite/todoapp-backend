package org.misterstorm.eventplatform.todoapp.auth.entrypoint.http

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:Email(message = "Informe um e-mail valido")
    @field:NotBlank(message = "E-mail e obrigatorio")
    val email: String,
    @field:NotBlank(message = "Nome e obrigatorio")
    @field:Size(min = 3, max = 120, message = "Nome deve ter entre 3 e 120 caracteres")
    val displayName: String,
    @field:NotBlank(message = "Senha e obrigatoria")
    @field:Size(min = 8, max = 120, message = "Senha deve ter entre 8 e 120 caracteres")
    val password: String,
)

data class LoginRequest(
    @field:Email(message = "Informe um e-mail valido")
    @field:NotBlank(message = "E-mail e obrigatorio")
    val email: String,
    @field:NotBlank(message = "Senha e obrigatoria")
    val password: String,
)

