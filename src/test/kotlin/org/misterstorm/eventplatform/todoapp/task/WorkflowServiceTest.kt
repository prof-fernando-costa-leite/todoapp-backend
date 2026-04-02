package org.misterstorm.eventplatform.todoapp.task

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.misterstorm.eventplatform.todoapp.board.application.service.BoardService
import org.misterstorm.eventplatform.todoapp.board.domain.BoardStatusRecord
import org.misterstorm.eventplatform.todoapp.board.domain.StatusKind
import org.misterstorm.eventplatform.todoapp.common.error.domain.ConflictException
import org.misterstorm.eventplatform.todoapp.common.error.domain.ValidationException
import org.misterstorm.eventplatform.todoapp.task.adapters.out.persistence.TaskRepository
import org.misterstorm.eventplatform.todoapp.task.application.service.WorkflowService
import org.misterstorm.eventplatform.todoapp.task.domain.TaskDetails
import org.misterstorm.eventplatform.todoapp.task.domain.TaskPriority
import java.time.Instant
import java.util.UUID

class WorkflowServiceTest {

    private val boardService = mockk<BoardService>()
    private val taskRepository = mockk<TaskRepository>()
    private val workflowService = WorkflowService(boardService, taskRepository)

    @Test
    fun `should reject transition not configured in board`() {
        val task = sampleTask(statusRank = 10, blockerTaskId = null)
        val targetStatusId = UUID.randomUUID()
        every { boardService.findStatusOrThrow(targetStatusId) } returns sampleStatus(task.boardId, targetStatusId, rank = 20)
        every { boardService.hasTransition(task.boardId, task.statusId, targetStatusId) } returns false

        assertThrows(ValidationException::class.java) {
            workflowService.validateStatusChange(task, targetStatusId)
        }
    }

    @Test
    fun `should reject advance when blocker task is still open`() {
        val blockerId = UUID.randomUUID()
        val task = sampleTask(statusRank = 10, blockerTaskId = blockerId)
        val targetStatusId = UUID.randomUUID()
        every { boardService.findStatusOrThrow(targetStatusId) } returns sampleStatus(task.boardId, targetStatusId, rank = 20)
        every { boardService.hasTransition(task.boardId, task.statusId, targetStatusId) } returns true
        every { taskRepository.findById(blockerId) } returns sampleTask(statusRank = 10, blockerTaskId = null, terminal = false)

        assertThrows(ConflictException::class.java) {
            workflowService.validateStatusChange(task, targetStatusId)
        }
    }

    @Test
    fun `should allow advance when blocker task is done`() {
        val blockerId = UUID.randomUUID()
        val task = sampleTask(statusRank = 10, blockerTaskId = blockerId)
        val targetStatusId = UUID.randomUUID()
        val targetStatus = sampleStatus(task.boardId, targetStatusId, rank = 20)
        every { boardService.findStatusOrThrow(targetStatusId) } returns targetStatus
        every { boardService.hasTransition(task.boardId, task.statusId, targetStatusId) } returns true
        every { taskRepository.findById(blockerId) } returns sampleTask(statusRank = 30, blockerTaskId = null, terminal = true)

        val resolved = workflowService.validateStatusChange(task, targetStatusId)

        assertEquals(targetStatusId, resolved.id)
    }

    private fun sampleTask(statusRank: Int, blockerTaskId: UUID?, terminal: Boolean = false): TaskDetails {
        val boardId = UUID.randomUUID()
        return TaskDetails(
            id = UUID.randomUUID(),
            boardId = boardId,
            groupId = UUID.randomUUID(),
            creatorId = UUID.randomUUID(),
            creatorDisplayName = "Owner User",
            assigneeId = null,
            assigneeDisplayName = null,
            title = "Task",
            description = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            statusId = UUID.randomUUID(),
            statusCode = if (terminal) "DONE" else "TODO",
            statusName = if (terminal) "Done" else "To do",
            statusRank = statusRank,
            statusTerminal = terminal,
            points = null,
            priority = TaskPriority.MEDIUM,
            blockingTaskId = blockerTaskId,
        )
    }

    private fun sampleStatus(boardId: UUID, id: UUID, rank: Int): BoardStatusRecord = BoardStatusRecord(
        id = id,
        boardId = boardId,
        code = "DOING",
        name = "Doing",
        kind = StatusKind.SYSTEM,
        rank = rank,
        isInitial = false,
        isTerminal = false,
        createdAt = Instant.now(),
    )
}

