package org.misterstorm.eventplatform.todoapp.task.domain

import java.time.Instant
import java.util.UUID

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

data class TaskRecord(
    val id: UUID,
    val boardId: UUID,
    val creatorId: UUID,
    val assigneeId: UUID?,
    val title: String,
    val description: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val statusId: UUID,
    val points: Int?,
    val priority: TaskPriority,
    val blockingTaskId: UUID?,
)

data class TaskStatusHistoryRecord(
    val id: UUID,
    val taskId: UUID,
    val fromStatusId: UUID?,
    val toStatusId: UUID,
    val changedBy: UUID,
    val changedAt: Instant,
)

data class TaskDetails(
    val id: UUID,
    val boardId: UUID,
    val groupId: UUID,
    val creatorId: UUID,
    val creatorDisplayName: String,
    val assigneeId: UUID?,
    val assigneeDisplayName: String?,
    val title: String,
    val description: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val statusId: UUID,
    val statusCode: String,
    val statusName: String,
    val statusRank: Int,
    val statusTerminal: Boolean,
    val points: Int?,
    val priority: TaskPriority,
    val blockingTaskId: UUID?,
)

data class TaskHistoryEntry(
    val id: UUID,
    val taskId: UUID,
    val fromStatusId: UUID?,
    val fromStatusName: String?,
    val toStatusId: UUID,
    val toStatusName: String,
    val changedBy: UUID,
    val changedAt: Instant,
)

data class TaskStatusResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val rank: Int,
    val terminal: Boolean,
)

data class TaskResponse(
    val id: UUID,
    val boardId: UUID,
    val creatorId: UUID,
    val creatorDisplayName: String,
    val assigneeId: UUID?,
    val assigneeDisplayName: String?,
    val title: String,
    val description: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val status: TaskStatusResponse,
    val points: Int?,
    val priority: TaskPriority,
    val blockerTaskId: UUID?,
)

data class TaskHistoryResponse(
    val id: UUID,
    val fromStatusId: UUID?,
    val fromStatusName: String?,
    val toStatusId: UUID,
    val toStatusName: String,
    val changedBy: UUID,
    val changedAt: Instant,
)

fun TaskDetails.toResponse() = TaskResponse(
    id = id,
    boardId = boardId,
    creatorId = creatorId,
    creatorDisplayName = creatorDisplayName,
    assigneeId = assigneeId,
    assigneeDisplayName = assigneeDisplayName,
    title = title,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = TaskStatusResponse(
        id = statusId,
        code = statusCode,
        name = statusName,
        rank = statusRank,
        terminal = statusTerminal,
    ),
    points = points,
    priority = priority,
    blockerTaskId = blockingTaskId,
)

