package org.misterstorm.eventplatform.todoapp.group.entrypoint.http

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.misterstorm.eventplatform.todoapp.group.domain.GroupMemberRole
import java.util.UUID

data class CreateGroupRequest(
    @field:NotBlank(message = "Nome do grupo e obrigatorio")
    @field:Size(min = 3, max = 120, message = "Nome do grupo deve ter entre 3 e 120 caracteres")
    val name: String,
    val memberIds: Set<UUID> = emptySet(),
)

data class AddGroupMemberRequest(
    val userId: UUID,
    val role: GroupMemberRole = GroupMemberRole.MEMBER,
)
