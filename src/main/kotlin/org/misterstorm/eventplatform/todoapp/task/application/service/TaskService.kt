package org.misterstorm.eventplatform.todoapp.task.application.service

import org.misterstorm.eventplatform.todoapp.auth.application.port.out.UserRepositoryPort
import org.misterstorm.eventplatform.todoapp.board.application.service.BoardService
import org.misterstorm.eventplatform.todoapp.common.error.domain.ForbiddenException
import org.misterstorm.eventplatform.todoapp.common.error.domain.NotFoundException
import org.misterstorm.eventplatform.todoapp.common.error.domain.ValidationException
import org.misterstorm.eventplatform.todoapp.common.logging.LoggingContextManager
import org.misterstorm.eventplatform.todoapp.common.security.service.CurrentUserProvider
import org.misterstorm.eventplatform.todoapp.group.adapters.out.persistence.GroupRepository
import org.misterstorm.eventplatform.todoapp.task.adapters.out.persistence.TaskRepository
import org.misterstorm.eventplatform.todoapp.task.domain.TaskDetails
import org.misterstorm.eventplatform.todoapp.task.domain.TaskHistoryResponse
import org.misterstorm.eventplatform.todoapp.task.domain.TaskRecord
import org.misterstorm.eventplatform.todoapp.task.domain.TaskResponse
import org.misterstorm.eventplatform.todoapp.task.domain.TaskStatusHistoryRecord
import org.misterstorm.eventplatform.todoapp.task.domain.toResponse
import org.misterstorm.eventplatform.todoapp.task.entrypoint.http.ChangeTaskStatusRequest
import org.misterstorm.eventplatform.todoapp.task.entrypoint.http.CreateTaskRequest
import org.misterstorm.eventplatform.todoapp.task.entrypoint.http.UpdateTaskAssigneeRequest
import org.misterstorm.eventplatform.todoapp.task.entrypoint.http.UpdateTaskBlockerRequest
import org.misterstorm.eventplatform.todoapp.task.entrypoint.http.UpdateTaskRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val boardService: BoardService,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepositoryPort,
    private val currentUserProvider: CurrentUserProvider,
    private val workflowService: WorkflowService,
    private val loggingContextManager: LoggingContextManager,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createTask(boardId: UUID, request: CreateTaskRequest): TaskResponse {
        val board = boardService.findBoardOrThrow(boardId)
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureBoardMember(board.groupId, currentUser.userId)
        request.assigneeId?.let { ensureAssignableUser(board.groupId, it) }
        val blockerTask = request.blockerTaskId?.let { blockerId ->
            taskRepository.findById(blockerId) ?: throw NotFoundException("Blocking task not found")
        }
        if (blockerTask != null && blockerTask.boardId != boardId) {
            throw ValidationException("The blocking task must belong to the same board")
        }

        val initialStatus = boardService.findInitialStatus(boardId)
        val now = Instant.now(clock)
        val task = TaskRecord(
            id = UUID.randomUUID(),
            boardId = boardId,
            creatorId = currentUser.userId,
            assigneeId = request.assigneeId,
            title = request.title.trim(),
            description = request.description?.trim()?.takeIf { it.isNotBlank() },
            createdAt = now,
            updatedAt = now,
            statusId = initialStatus.id,
            points = request.points,
            priority = request.priority,
            blockingTaskId = request.blockerTaskId,
        )
        loggingContextManager.withBoard(boardId) {
            taskRepository.insert(task)
            taskRepository.insertHistory(
                TaskStatusHistoryRecord(
                    id = UUID.randomUUID(),
                    taskId = task.id,
                    fromStatusId = null,
                    toStatusId = initialStatus.id,
                    changedBy = currentUser.userId,
                    changedAt = now,
                ),
            )
            logger.info("Task created on board")
        }
        return getTask(task.id)
    }

    fun listBoardTasks(boardId: UUID): List<TaskResponse> {
        val board = boardService.findBoardOrThrow(boardId)
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureBoardMember(board.groupId, currentUser.userId)
        return loggingContextManager.withBoard(boardId) {
            taskRepository.listByBoard(boardId).map { it.toResponse() }
        }
    }

    fun listMyTasks(): List<TaskResponse> {
        val currentUser = currentUserProvider.requireCurrentUser()
        return taskRepository.listMine(currentUser.userId).filter { canReadTask(currentUser.userId, it) }.map { it.toResponse() }
    }

    fun getTask(taskId: UUID): TaskResponse {
        val currentUser = currentUserProvider.requireCurrentUser()
        val task = taskRepository.findById(taskId) ?: throw NotFoundException("Task not found")
        ensureTaskReadable(currentUser.userId, task)
        return loggingContextManager.withBoard(task.boardId) { task.toResponse() }
    }

    @Transactional
    fun updateTask(taskId: UUID, request: UpdateTaskRequest): TaskResponse {
        val currentUser = currentUserProvider.requireCurrentUser()
        val task = taskRepository.findById(taskId) ?: throw NotFoundException("Task not found")
        ensureTaskManageable(currentUser.userId, task)
        val now = Instant.now(clock)
        loggingContextManager.withBoard(task.boardId) {
            taskRepository.updateTask(
                taskId = taskId,
                title = request.title?.trim() ?: task.title,
                description = request.description?.trim() ?: task.description,
                points = request.points ?: task.points,
                priority = request.priority ?: task.priority,
                updatedAt = now,
            )

            if (request.assigneeIdProvided) {
                request.assigneeId?.let { ensureAssignableUser(task.groupId, it) }
                taskRepository.updateAssignee(taskId, request.assigneeId, now)
            }

            if (request.blockerTaskIdProvided) {
                val blockerId = request.blockerTaskId
                if (blockerId == taskId) {
                    throw ValidationException("A task cannot block itself")
                }
                blockerId?.let {
                    val blocker = taskRepository.findById(it) ?: throw NotFoundException("Blocking task not found")
                    if (blocker.boardId != task.boardId) {
                        throw ValidationException("The blocking task must belong to the same board")
                    }
                }
                taskRepository.updateBlocker(taskId, blockerId, now)
            }

            logger.info("Task updated")
        }
        return getTask(taskId)
    }

    @Transactional
    fun changeStatus(taskId: UUID, request: ChangeTaskStatusRequest): TaskResponse {
        val currentUser = currentUserProvider.requireCurrentUser()
        val task = taskRepository.findById(taskId) ?: throw NotFoundException("Task not found")
        ensureTaskManageable(currentUser.userId, task)
        val statusId = request.statusId ?: throw ValidationException("statusId is required")
        val targetStatus = workflowService.validateStatusChange(task, statusId)
        val now = Instant.now(clock)
        loggingContextManager.withBoard(task.boardId) {
            taskRepository.updateStatus(taskId, targetStatus.id, now)
            taskRepository.insertHistory(
                TaskStatusHistoryRecord(
                    id = UUID.randomUUID(),
                    taskId = taskId,
                    fromStatusId = task.statusId,
                    toStatusId = targetStatus.id,
                    changedBy = currentUser.userId,
                    changedAt = now,
                ),
            )
            logger.info("Task status changed")
        }
        return getTask(taskId)
    }

    @Transactional
    fun updateAssignee(taskId: UUID, request: UpdateTaskAssigneeRequest): TaskResponse {
        val currentUser = currentUserProvider.requireCurrentUser()
        val task = taskRepository.findById(taskId) ?: throw NotFoundException("Task not found")
        ensureTaskManageable(currentUser.userId, task)
        request.assigneeId?.let { ensureAssignableUser(task.groupId, it) }
        val now = Instant.now(clock)
        loggingContextManager.withBoard(task.boardId) {
            taskRepository.updateAssignee(taskId, request.assigneeId, now)
            logger.info("Task assignee changed")
        }
        return getTask(taskId)
    }

    @Transactional
    fun updateBlocker(taskId: UUID, request: UpdateTaskBlockerRequest): TaskResponse {
        val currentUser = currentUserProvider.requireCurrentUser()
        val task = taskRepository.findById(taskId) ?: throw NotFoundException("Task not found")
        ensureTaskManageable(currentUser.userId, task)
        val blockerId = request.blockerTaskId
        if (blockerId == taskId) {
            throw ValidationException("A task cannot block itself")
        }
        blockerId?.let {
            val blocker = taskRepository.findById(it) ?: throw NotFoundException("Blocking task not found")
            if (blocker.boardId != task.boardId) {
                throw ValidationException("The blocking task must belong to the same board")
            }
        }
        val now = Instant.now(clock)
        loggingContextManager.withBoard(task.boardId) {
            taskRepository.updateBlocker(taskId, blockerId, now)
            logger.info("Task blocker changed")
        }
        return getTask(taskId)
    }

    fun getHistory(taskId: UUID): List<TaskHistoryResponse> {
        val currentUser = currentUserProvider.requireCurrentUser()
        val task = taskRepository.findById(taskId) ?: throw NotFoundException("Task not found")
        ensureTaskReadable(currentUser.userId, task)
        return loggingContextManager.withBoard(task.boardId) {
            taskRepository.listHistory(taskId).map {
                TaskHistoryResponse(
                    id = it.id,
                    fromStatusId = it.fromStatusId,
                    fromStatusName = it.fromStatusName,
                    toStatusId = it.toStatusId,
                    toStatusName = it.toStatusName,
                    changedBy = it.changedBy,
                    changedAt = it.changedAt,
                )
            }
        }
    }

    @Transactional
    fun deleteTask(taskId: UUID) {
        val currentUser = currentUserProvider.requireCurrentUser()
        val task = taskRepository.findById(taskId) ?: throw NotFoundException("Task not found")
        ensureTaskManageable(currentUser.userId, task)
        val now = Instant.now(clock)
        loggingContextManager.withBoard(task.boardId) {
            taskRepository.clearBlockerReferences(taskId, now)
            taskRepository.deleteById(taskId)
            logger.info("Task removed from board")
        }
    }

    private fun ensureAssignableUser(groupId: UUID, assigneeId: UUID) {
        if (!userRepository.existsById(assigneeId)) {
            throw NotFoundException("Assignee user not found")
        }
        if (!groupRepository.isMember(groupId, assigneeId)) {
            throw ValidationException("The assignee must belong to the board's group")
        }
    }

    private fun ensureBoardMember(groupId: UUID, userId: UUID) {
        if (!groupRepository.isMember(groupId, userId)) {
            throw ForbiddenException("User does not belong to the board's group")
        }
    }

    private fun ensureTaskReadable(userId: UUID, task: TaskDetails) {
        if (!canReadTask(userId, task)) {
            throw ForbiddenException("User cannot view this task")
        }
    }

    private fun ensureTaskManageable(userId: UUID, task: TaskDetails) {
        if (!groupRepository.isMember(task.groupId, userId)) {
            throw ForbiddenException("Only group members can modify tasks on the board")
        }
    }

    private fun canReadTask(userId: UUID, task: TaskDetails): Boolean =
        task.creatorId == userId || task.assigneeId == userId || groupRepository.isMember(task.groupId, userId)
}
