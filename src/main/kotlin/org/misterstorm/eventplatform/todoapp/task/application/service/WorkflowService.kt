package org.misterstorm.eventplatform.todoapp.task.application.service

import org.misterstorm.eventplatform.todoapp.board.application.service.BoardService
import org.misterstorm.eventplatform.todoapp.board.domain.BoardStatusRecord
import org.misterstorm.eventplatform.todoapp.common.error.domain.ConflictException
import org.misterstorm.eventplatform.todoapp.common.error.domain.ValidationException
import org.misterstorm.eventplatform.todoapp.task.adapters.out.persistence.TaskRepository
import org.misterstorm.eventplatform.todoapp.task.domain.TaskDetails
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WorkflowService(
    private val boardService: BoardService,
    private val taskRepository: TaskRepository,
) {

    fun validateStatusChange(task: TaskDetails, targetStatusId: UUID): BoardStatusRecord {
        if (task.statusId == targetStatusId) {
            throw ValidationException("A task ja esta no status informado")
        }
        val targetStatus = boardService.findStatusOrThrow(targetStatusId)
        if (targetStatus.boardId != task.boardId) {
            throw ValidationException("O status de destino nao pertence ao board da task")
        }
        if (!boardService.hasTransition(task.boardId, task.statusId, targetStatusId)) {
            throw ValidationException("Transicao de status nao permitida para este board")
        }
        val blockerTask = task.blockingTaskId?.let { blockerId ->
            taskRepository.findById(blockerId) ?: throw ValidationException("Task bloqueante nao encontrada")
        }
        if (blockerTask != null && !blockerTask.statusTerminal && targetStatus.rank > task.statusRank) {
            throw ConflictException("A task esta bloqueada por outra task em aberto e nao pode avancar")
        }
        return targetStatus
    }
}
