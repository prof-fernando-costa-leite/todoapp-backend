package org.misterstorm.eventplatform.todoapp.group.domain

import java.time.Instant
import java.util.UUID

enum class GroupMemberRole {
    OWNER,
    MEMBER,
}

data class GroupRecord(
    val id: UUID,
    val name: String,
    val createdBy: UUID,
    val createdAt: Instant,
)

data class GroupMemberRecord(
    val groupId: UUID,
    val userId: UUID,
    val role: GroupMemberRole,
    val addedAt: Instant,
    val displayName: String? = null,
)

data class GroupSummaryResponse(
    val id: UUID,
    val name: String,
    val createdBy: UUID,
    val createdAt: Instant,
)

data class GroupMemberResponse(
    val userId: UUID,
    val displayName: String,
    val role: GroupMemberRole,
    val addedAt: Instant,
)

data class GroupDetailsResponse(
    val id: UUID,
    val name: String,
    val createdBy: UUID,
    val createdAt: Instant,
    val members: List<GroupMemberResponse>,
)
