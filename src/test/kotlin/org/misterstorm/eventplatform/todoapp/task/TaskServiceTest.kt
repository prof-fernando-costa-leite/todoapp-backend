package org.misterstorm.eventplatform.todoapp.task

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.misterstorm.eventplatform.todoapp.auth.application.port.out.UserRepositoryPort
import org.misterstorm.eventplatform.todoapp.board.application.service.BoardService
import org.misterstorm.eventplatform.todoapp.board.domain.BoardRecord
import org.misterstorm.eventplatform.todoapp.common.error.domain.ForbiddenException
import org.misterstorm.eventplatform.todoapp.common.error.domain.NotFoundException
import org.misterstorm.eventplatform.todoapp.common.error.domain.ValidationException
import org.misterstorm.eventplatform.todoapp.common.logging.LoggingContextManager
import org.misterstorm.eventplatform.todoapp.common.security.model.AuthenticatedUser
import org.misterstorm.eventplatform.todoapp.common.security.service.CurrentUserProvider
import org.misterstorm.eventplatform.todoapp.group.adapters.out.persistence.GroupRepository
import org.misterstorm.eventplatform.todoapp.task.adapters.out.persistence.TaskRepository
import org.misterstorm.eventplatform.todoapp.task.application.service.TaskService
import org.misterstorm.eventplatform.todoapp.task.application.service.WorkflowService
import org.misterstorm.eventplatform.todoapp.task.domain.TaskDetails
import org.misterstorm.eventplatform.todoapp.task.domain.TaskPriority
import org.misterstorm.eventplatform.todoapp.task.entrypoint.http.CreateTaskRequest
import org.misterstorm.eventplatform.todoapp.task.entrypoint.http.UpdateTaskAssigneeRequest
import org.misterstorm.eventplatform.todoapp.task.entrypoint.http.UpdateTaskBlockerRequest
import org.misterstorm.eventplatform.todoapp.task.entrypoint.http.UpdateTaskRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class TaskServiceTest {

    private val taskRepository = mockk<TaskRepository>()
    private val boardService = mockk<BoardService>()
    private val groupRepository = mockk<GroupRepository>()
    private val userRepository = mockk<UserRepositoryPort>()
    private val currentUserProvider = mockk<CurrentUserProvider>()
    private val workflowService = mockk<WorkflowService>()
    private val loggingContextManager = LoggingContextManager()
    private val clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val taskService = TaskService(
        taskRepository = taskRepository,
        boardService = boardService,
        groupRepository = groupRepository,
        userRepository = userRepository,
        currentUserProvider = currentUserProvider,
        workflowService = workflowService,
        loggingContextManager = loggingContextManager,
        clock = clock,
    )

    // ─── updateTask ──────────────────────────────────────────────────────────

    @Test
    fun `updateTask should throw NotFoundException when task does not exist`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns null

        assertThrows(NotFoundException::class.java) {
            taskService.updateTask(taskId, UpdateTaskRequest().apply { title = "New title" })
        }
    }

    @Test
    fun `updateTask should throw ForbiddenException when user is not a group member`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val task = sampleTask(taskId = taskId, creatorId = UUID.randomUUID())
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns task
        every { groupRepository.isMember(task.groupId, userId) } returns false

        assertThrows(ForbiddenException::class.java) {
            taskService.updateTask(taskId, UpdateTaskRequest().apply { title = "New title" })
        }
    }

    @Test
    fun `updateTask should update task when user is a group member`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val task = sampleTask(taskId = taskId, creatorId = userId)
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns task
        every { groupRepository.isMember(task.groupId, userId) } returns true
        every { taskRepository.updateTask(any(), any(), any(), any(), any(), any()) } just runs
        every { taskRepository.findById(taskId) } returns task

        taskService.updateTask(taskId, UpdateTaskRequest().apply { title = "New title" })

        verify { taskRepository.updateTask(taskId, "New title", any(), any(), any(), any()) }
    }

    @Test
    fun `updateTask should remove assignee when assigneeId is explicitly null`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val task = sampleTask(taskId = taskId, creatorId = userId)

        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns task
        every { groupRepository.isMember(task.groupId, userId) } returns true
        every { taskRepository.updateTask(any(), any(), any(), any(), any(), any()) } just runs
        every { taskRepository.updateAssignee(any(), any(), any()) } just runs

        val request = UpdateTaskRequest().apply {
            assigneeId = null
        }

        taskService.updateTask(taskId, request)

        verify { taskRepository.updateAssignee(taskId, null, any()) }
    }

    // ─── updateAssignee ───────────────────────────────────────────────────────

    @Test
    fun `updateAssignee should throw NotFoundException when task does not exist`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns null

        assertThrows(NotFoundException::class.java) {
            taskService.updateAssignee(taskId, UpdateTaskAssigneeRequest(assigneeId = UUID.randomUUID()))
        }
    }

    @Test
    fun `updateAssignee should throw NotFoundException when assignee does not exist`() {
        val userId = UUID.randomUUID()
        val assigneeId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val task = sampleTask(taskId = taskId, creatorId = userId)
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns task
        every { groupRepository.isMember(task.groupId, userId) } returns true
        every { userRepository.existsById(assigneeId) } returns false

        assertThrows(NotFoundException::class.java) {
            taskService.updateAssignee(taskId, UpdateTaskAssigneeRequest(assigneeId = assigneeId))
        }
    }

    @Test
    fun `updateAssignee should throw ValidationException when assignee is not a group member`() {
        val userId = UUID.randomUUID()
        val assigneeId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val task = sampleTask(taskId = taskId, creatorId = userId)
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns task
        every { groupRepository.isMember(task.groupId, userId) } returns true
        every { userRepository.existsById(assigneeId) } returns true
        every { groupRepository.isMember(task.groupId, assigneeId) } returns false

        assertThrows(ValidationException::class.java) {
            taskService.updateAssignee(taskId, UpdateTaskAssigneeRequest(assigneeId = assigneeId))
        }
    }

    // ─── updateBlocker ────────────────────────────────────────────────────────

    @Test
    fun `updateBlocker should throw NotFoundException when task does not exist`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns null

        assertThrows(NotFoundException::class.java) {
            taskService.updateBlocker(taskId, UpdateTaskBlockerRequest(blockerTaskId = UUID.randomUUID()))
        }
    }

    @Test
    fun `updateBlocker should throw ValidationException when task blocks itself`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val task = sampleTask(taskId = taskId, creatorId = userId)
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns task
        every { groupRepository.isMember(task.groupId, userId) } returns true

        assertThrows(ValidationException::class.java) {
            taskService.updateBlocker(taskId, UpdateTaskBlockerRequest(blockerTaskId = taskId))
        }
    }

    @Test
    fun `updateBlocker should throw NotFoundException when blocker task does not exist`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val blockerId = UUID.randomUUID()
        val task = sampleTask(taskId = taskId, creatorId = userId)
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns task
        every { groupRepository.isMember(task.groupId, userId) } returns true
        every { taskRepository.findById(blockerId) } returns null

        assertThrows(NotFoundException::class.java) {
            taskService.updateBlocker(taskId, UpdateTaskBlockerRequest(blockerTaskId = blockerId))
        }
    }

    @Test
    fun `updateBlocker should throw ValidationException when blocker is in a different board`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val blockerId = UUID.randomUUID()
        val task = sampleTask(taskId = taskId, creatorId = userId)
        val blocker = sampleTask(taskId = blockerId, boardId = UUID.randomUUID(), creatorId = userId)
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns task
        every { groupRepository.isMember(task.groupId, userId) } returns true
        every { taskRepository.findById(blockerId) } returns blocker

        assertThrows(ValidationException::class.java) {
            taskService.updateBlocker(taskId, UpdateTaskBlockerRequest(blockerTaskId = blockerId))
        }
    }

    @Test
    fun `updateBlocker should remove blocker when null is provided`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val task = sampleTask(taskId = taskId, creatorId = userId, blockerId = UUID.randomUUID())
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns task
        every { groupRepository.isMember(task.groupId, userId) } returns true
        every { taskRepository.updateBlocker(any(), null, any()) } just runs

        taskService.updateBlocker(taskId, UpdateTaskBlockerRequest(blockerTaskId = null))

        verify { taskRepository.updateBlocker(taskId, null, any()) }
    }

    // ─── getTask ──────────────────────────────────────────────────────────────

    @Test
    fun `getTask should throw NotFoundException when task does not exist`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns null

        assertThrows(NotFoundException::class.java) {
            taskService.getTask(taskId)
        }
    }

    @Test
    fun `getTask should throw ForbiddenException for unrelated user`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val task = sampleTask(taskId = taskId, creatorId = UUID.randomUUID())
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns task
        every { groupRepository.isMember(task.groupId, userId) } returns false

        assertThrows(ForbiddenException::class.java) {
            taskService.getTask(taskId)
        }
    }

    // ─── listBoardTasks ───────────────────────────────────────────────────────

    @Test
    fun `listBoardTasks should throw ForbiddenException for non-member`() {
        val userId = UUID.randomUUID()
        val boardId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        every { boardService.findBoardOrThrow(boardId) } returns sampleBoard(boardId, groupId)
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { groupRepository.isMember(groupId, userId) } returns false

        assertThrows(ForbiddenException::class.java) {
            taskService.listBoardTasks(boardId)
        }
    }

    // ─── deleteTask ───────────────────────────────────────────────────────────

    @Test
    fun `deleteTask should throw NotFoundException when task does not exist`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns null

        assertThrows(NotFoundException::class.java) {
            taskService.deleteTask(taskId)
        }
    }

    @Test
    fun `deleteTask should throw ForbiddenException when user is not a group member`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val task = sampleTask(taskId = taskId)
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns task
        every { groupRepository.isMember(task.groupId, userId) } returns false

        assertThrows(ForbiddenException::class.java) {
            taskService.deleteTask(taskId)
        }
    }

    @Test
    fun `deleteTask should clear blocker references and delete task when user is a group member`() {
        val userId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val task = sampleTask(taskId = taskId, creatorId = userId)
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { taskRepository.findById(taskId) } returns task
        every { groupRepository.isMember(task.groupId, userId) } returns true
        every { taskRepository.clearBlockerReferences(taskId, any()) } just runs
        every { taskRepository.deleteById(taskId) } returns 1

        taskService.deleteTask(taskId)

        verify { taskRepository.clearBlockerReferences(taskId, any()) }
        verify { taskRepository.deleteById(taskId) }
    }

    // ─── createTask ───────────────────────────────────────────────────────────

    @Test
    fun `createTask should throw NotFoundException when blocker task does not exist`() {
        val userId = UUID.randomUUID()
        val boardId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        val blockerId = UUID.randomUUID()
        every { boardService.findBoardOrThrow(boardId) } returns sampleBoard(boardId, groupId)
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { groupRepository.isMember(groupId, userId) } returns true
        every { taskRepository.findById(blockerId) } returns null

        assertThrows(NotFoundException::class.java) {
            taskService.createTask(boardId, CreateTaskRequest(title = "Task", blockerTaskId = blockerId))
        }
    }

    @Test
    fun `createTask should throw ValidationException when blocker is in a different board`() {
        val userId = UUID.randomUUID()
        val boardId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        val blockerId = UUID.randomUUID()
        val blockerTask = sampleTask(taskId = blockerId, boardId = UUID.randomUUID(), creatorId = userId)
        every { boardService.findBoardOrThrow(boardId) } returns sampleBoard(boardId, groupId)
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { groupRepository.isMember(groupId, userId) } returns true
        every { taskRepository.findById(blockerId) } returns blockerTask

        assertThrows(ValidationException::class.java) {
            taskService.createTask(boardId, CreateTaskRequest(title = "Task", blockerTaskId = blockerId))
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private fun sampleTask(
        taskId: UUID = UUID.randomUUID(),
        boardId: UUID = UUID.randomUUID(),
        groupId: UUID = UUID.randomUUID(),
        creatorId: UUID = UUID.randomUUID(),
        blockerId: UUID? = null,
    ) = TaskDetails(
        id = taskId,
        boardId = boardId,
        groupId = groupId,
        creatorId = creatorId,
        creatorDisplayName = "Owner User",
        assigneeId = null,
        assigneeDisplayName = null,
        title = "Task",
        description = null,
        createdAt = Instant.parse("2026-01-01T10:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
        statusId = UUID.randomUUID(),
        statusCode = "TODO",
        statusName = "To Do",
        statusRank = 10,
        statusTerminal = false,
        points = null,
        priority = TaskPriority.MEDIUM,
        blockingTaskId = blockerId,
    )

    private fun sampleBoard(boardId: UUID, groupId: UUID) = BoardRecord(
        id = boardId,
        groupId = groupId,
        name = "Board",
        description = null,
        createdBy = UUID.randomUUID(),
        createdAt = Instant.parse("2026-01-01T10:00:00Z"),
    )
}

