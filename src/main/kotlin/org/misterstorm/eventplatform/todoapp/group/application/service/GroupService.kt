package org.misterstorm.eventplatform.todoapp.group.application.service

import org.misterstorm.eventplatform.todoapp.auth.application.port.out.UserRepositoryPort
import org.misterstorm.eventplatform.todoapp.common.error.domain.ForbiddenException
import org.misterstorm.eventplatform.todoapp.common.error.domain.NotFoundException
import org.misterstorm.eventplatform.todoapp.common.security.service.CurrentUserProvider
import org.misterstorm.eventplatform.todoapp.group.adapters.out.persistence.GroupRepository
import org.misterstorm.eventplatform.todoapp.group.domain.GroupDetailsResponse
import org.misterstorm.eventplatform.todoapp.group.domain.GroupMemberRecord
import org.misterstorm.eventplatform.todoapp.group.domain.GroupMemberResponse
import org.misterstorm.eventplatform.todoapp.group.domain.GroupMemberRole
import org.misterstorm.eventplatform.todoapp.group.domain.GroupRecord
import org.misterstorm.eventplatform.todoapp.group.domain.GroupSummaryResponse
import org.misterstorm.eventplatform.todoapp.group.entrypoint.http.AddGroupMemberRequest
import org.misterstorm.eventplatform.todoapp.group.entrypoint.http.CreateGroupRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepositoryPort,
    private val currentUserProvider: CurrentUserProvider,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createGroup(request: CreateGroupRequest): GroupDetailsResponse {
        val currentUser = currentUserProvider.requireCurrentUser()
        val memberIds = request.memberIds + currentUser.userId
        memberIds.forEach {
            if (!userRepository.existsById(it)) {
                throw NotFoundException("Usuario $it nao encontrado")
            }
        }

        val group = GroupRecord(
            id = UUID.randomUUID(),
            name = request.name.trim(),
            createdBy = currentUser.userId,
            createdAt = Instant.now(clock),
        )
        groupRepository.insertGroup(group)
        memberIds.forEach { memberId ->
            groupRepository.addMember(
                GroupMemberRecord(
                    groupId = group.id,
                    userId = memberId,
                    role = if (memberId == currentUser.userId) GroupMemberRole.OWNER else GroupMemberRole.MEMBER,
                    addedAt = Instant.now(clock),
                    displayName = null,
                ),
            )
        }
        logger.info("Grupo criado com sucesso")
        return getGroup(group.id)
    }

    fun listGroups(): List<GroupSummaryResponse> = groupRepository.listByUser(currentUserProvider.requireCurrentUser().userId)
        .map {
            GroupSummaryResponse(
                id = it.id,
                name = it.name,
                createdBy = it.createdBy,
                createdAt = it.createdAt,
            )
        }

    fun getGroup(groupId: UUID): GroupDetailsResponse {
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureMember(groupId, currentUser.userId)
        val group = groupRepository.findById(groupId) ?: throw NotFoundException("Grupo nao encontrado")
        return GroupDetailsResponse(
            id = group.id,
            name = group.name,
            createdBy = group.createdBy,
            createdAt = group.createdAt,
            members = groupRepository.listMembers(groupId).map {
                GroupMemberResponse(
                    userId = it.userId,
                    displayName = it.displayName ?: "",
                    role = it.role,
                    addedAt = it.addedAt,
                )
            },
        )
    }

    @Transactional
    fun addMember(groupId: UUID, request: AddGroupMemberRequest): GroupDetailsResponse {
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureOwner(groupId, currentUser.userId)
        if (!userRepository.existsById(request.userId)) {
            throw NotFoundException("Usuario nao encontrado")
        }
        groupRepository.addMember(
            GroupMemberRecord(
                groupId = groupId,
                userId = request.userId,
                role = request.role,
                addedAt = Instant.now(clock),
                displayName = null,
            ),
        )
        logger.info("Membro adicionado ao grupo")
        return getGroup(groupId)
    }

    @Transactional
    fun removeMember(groupId: UUID, userId: UUID) {
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureOwner(groupId, currentUser.userId)
        if (userId == currentUser.userId) {
            throw ForbiddenException("O dono do grupo nao pode remover a si mesmo")
        }
        groupRepository.removeMember(groupId, userId)
        logger.info("Membro removido do grupo")
    }

    fun ensureMember(groupId: UUID, userId: UUID) {
        if (!groupRepository.isMember(groupId, userId)) {
            throw ForbiddenException("Usuario nao pertence ao grupo informado")
        }
    }

    private fun ensureOwner(groupId: UUID, userId: UUID) {
        if (!groupRepository.isOwner(groupId, userId)) {
            throw ForbiddenException("Apenas o dono do grupo pode realizar esta acao")
        }
    }
}
