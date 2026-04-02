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
            throw ValidationException("The task is already in the provided status")
        }
        val targetStatus = boardService.findStatusOrThrow(targetStatusId)
        if (targetStatus.boardId != task.boardId) {
            throw ValidationException("The target status does not belong to the task's board")
        }
        if (!boardService.hasTransition(task.boardId, task.statusId, targetStatusId)) {
            throw ValidationException("Status transition not allowed for this board")
        }
        val blockerTask = task.blockingTaskId?.let { blockerId ->
            taskRepository.findById(blockerId) ?: throw ValidationException("Blocking task not found")
        }
        if (blockerTask != null && !blockerTask.statusTerminal && targetStatus.rank > task.statusRank) {
            throw ConflictException("The task is blocked by another open task and cannot advance")
        }
        return targetStatus
    }
}
