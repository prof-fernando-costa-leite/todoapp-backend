package org.misterstorm.eventplatform.todoapp.board.entrypoint.http

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateBoardRequest(
    @field:NotBlank(message = "Board name is required")
    @field:Size(min = 3, max = 120, message = "Board name must be between 3 and 120 characters")
    val name: String,
    @field:Size(max = 500, message = "Description must have at most 500 characters")
    val description: String? = null,
)

data class CreateBoardStatusRequest(
    @field:NotBlank(message = "Status code is required")
    @field:Size(min = 2, max = 40, message = "Status code must be between 2 and 40 characters")
    val code: String,
    @field:NotBlank(message = "Status name is required")
    @field:Size(min = 2, max = 80, message = "Status name must be between 2 and 80 characters")
    val name: String,
    @field:Min(value = 0, message = "Rank must be at least 0")
    @field:Max(value = 1000, message = "Rank must be at most 1000")
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

