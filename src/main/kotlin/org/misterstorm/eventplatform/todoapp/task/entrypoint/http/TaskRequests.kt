package org.misterstorm.eventplatform.todoapp.task.entrypoint.http

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.misterstorm.eventplatform.todoapp.task.domain.TaskPriority
import java.util.UUID

data class CreateTaskRequest(
    @field:NotBlank(message = "Titulo e obrigatorio")
    @field:Size(min = 3, max = 200, message = "Titulo deve ter entre 3 e 200 caracteres")
    val title: String,
    @field:Size(max = 2000, message = "Descricao deve ter no maximo 2000 caracteres")
    val description: String? = null,
    val assigneeId: UUID? = null,
    @field:Min(value = 0, message = "Pontuacao deve ser no minimo 0")
    @field:Max(value = 1000, message = "Pontuacao deve ser no maximo 1000")
    val points: Int? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val blockerTaskId: UUID? = null,
)

class UpdateTaskRequest {
    @field:Size(min = 3, max = 200, message = "Titulo deve ter entre 3 e 200 caracteres")
    var title: String? = null

    @field:Size(max = 2000, message = "Descricao deve ter no maximo 2000 caracteres")
    var description: String? = null

    @field:Min(value = 0, message = "Pontuacao deve ser no minimo 0")
    @field:Max(value = 1000, message = "Pontuacao deve ser no maximo 1000")
    var points: Int? = null

    var priority: TaskPriority? = null

    var assigneeId: UUID? = null
        set(value) {
            field = value
            assigneeIdProvided = true
        }

    var blockerTaskId: UUID? = null
        set(value) {
            field = value
            blockerTaskIdProvided = true
        }

    var assigneeIdProvided: Boolean = false
        private set

    var blockerTaskIdProvided: Boolean = false
        private set
}

data class ChangeTaskStatusRequest(
    @field:NotNull(message = "statusId e obrigatorio")
    val statusId: UUID? = null,
)

data class UpdateTaskAssigneeRequest(
    val assigneeId: UUID?,
)

data class UpdateTaskBlockerRequest(
    val blockerTaskId: UUID?,
)
