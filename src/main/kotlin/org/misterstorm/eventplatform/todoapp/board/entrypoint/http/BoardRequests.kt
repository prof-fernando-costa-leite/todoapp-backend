package org.misterstorm.eventplatform.todoapp.board.entrypoint.http

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateBoardRequest(
    @field:NotBlank(message = "Nome do board e obrigatorio")
    @field:Size(min = 3, max = 120, message = "Nome do board deve ter entre 3 e 120 caracteres")
    val name: String,
    @field:Size(max = 500, message = "Descricao deve ter no maximo 500 caracteres")
    val description: String? = null,
)

data class CreateBoardStatusRequest(
    @field:NotBlank(message = "Codigo do status e obrigatorio")
    @field:Size(min = 2, max = 40, message = "Codigo do status deve ter entre 2 e 40 caracteres")
    val code: String,
    @field:NotBlank(message = "Nome do status e obrigatorio")
    @field:Size(min = 2, max = 80, message = "Nome do status deve ter entre 2 e 80 caracteres")
    val name: String,
    @field:Min(value = 0, message = "Rank deve ser no minimo 0")
    @field:Max(value = 1000, message = "Rank deve ser no maximo 1000")
    val rank: Int,
    val initial: Boolean = false,
    val terminal: Boolean = false,
)

data class ReplaceTransitionsRequest(
    val transitions: List<TransitionPairRequest>,
)

data class TransitionPairRequest(
    val fromStatusId: UUID,
    val toStatusId: UUID,
)

