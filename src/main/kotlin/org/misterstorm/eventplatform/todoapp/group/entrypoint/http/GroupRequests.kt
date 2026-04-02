package org.misterstorm.eventplatform.todoapp.group.entrypoint.http

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.misterstorm.eventplatform.todoapp.group.domain.GroupMemberRole
import java.util.UUID

data class CreateGroupRequest(
    @field:NotBlank(message = "Group name is required")
    @field:Size(min = 3, max = 120, message = "Group name must be between 3 and 120 characters")
    val name: String,
    val memberIds: Set<UUID> = emptySet(),
)

data class AddGroupMemberRequest(
    val userId: UUID,
    val role: GroupMemberRole = GroupMemberRole.MEMBER,
)
