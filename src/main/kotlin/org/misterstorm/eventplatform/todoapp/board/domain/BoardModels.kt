package org.misterstorm.eventplatform.todoapp.board.domain

import org.misterstorm.eventplatform.todoapp.group.domain.GroupMemberRole
import java.time.Instant
import java.util.UUID

enum class StatusKind {
    SYSTEM,
    CUSTOM,
}

data class BoardRecord(
    val id: UUID,
    val groupId: UUID,
    val name: String,
    val description: String?,
    val createdBy: UUID,
    val createdAt: Instant,
)

data class BoardStatusRecord(
    val id: UUID,
    val boardId: UUID,
    val code: String,
    val name: String,
    val kind: StatusKind,
    val rank: Int,
    val isInitial: Boolean,
    val isTerminal: Boolean,
    val createdAt: Instant,
)

data class BoardTransitionRecord(
    val boardId: UUID,
    val fromStatusId: UUID,
    val toStatusId: UUID,
)

data class BoardResponse(
    val id: UUID,
    val groupId: UUID,
    val name: String,
    val description: String?,
    val createdBy: UUID,
    val createdAt: Instant,
)

data class BoardMemberResponse(
    val id: UUID,
    val userId: UUID,
    val displayName: String,
    val role: GroupMemberRole,
)

data class BoardDetailsResponse(
    val id: UUID,
    val groupId: UUID,
    val name: String,
    val description: String?,
    val createdBy: UUID,
    val createdAt: Instant,
    val members: List<BoardMemberResponse>,
    val statuses: List<BoardStatusResponse>,
    val transitions: List<BoardTransitionResponse>,
)

data class BoardStatusResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val kind: StatusKind,
    val rank: Int,
    val initial: Boolean,
    val terminal: Boolean,
)

data class BoardTransitionResponse(
    val fromStatusId: UUID,
    val toStatusId: UUID,
)

fun BoardRecord.toResponse() = BoardResponse(
    id = id,
    groupId = groupId,
    name = name,
    description = description,
    createdBy = createdBy,
    createdAt = createdAt,
)

fun BoardStatusRecord.toResponse() = BoardStatusResponse(
    id = id,
    code = code,
    name = name,
    kind = kind,
    rank = rank,
    initial = isInitial,
    terminal = isTerminal,
)
