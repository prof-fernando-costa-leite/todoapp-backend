package org.misterstorm.eventplatform.todoapp.group

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.misterstorm.eventplatform.todoapp.auth.application.port.out.UserRepositoryPort
import org.misterstorm.eventplatform.todoapp.common.error.domain.ForbiddenException
import org.misterstorm.eventplatform.todoapp.common.error.domain.NotFoundException
import org.misterstorm.eventplatform.todoapp.common.security.model.AuthenticatedUser
import org.misterstorm.eventplatform.todoapp.common.security.service.CurrentUserProvider
import org.misterstorm.eventplatform.todoapp.group.adapters.out.persistence.GroupRepository
import org.misterstorm.eventplatform.todoapp.group.application.service.GroupService
import org.misterstorm.eventplatform.todoapp.group.entrypoint.http.AddGroupMemberRequest
import org.misterstorm.eventplatform.todoapp.group.entrypoint.http.CreateGroupRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class GroupServiceTest {

    private val groupRepository = mockk<GroupRepository>()
    private val userRepository = mockk<UserRepositoryPort>()
    private val currentUserProvider = mockk<CurrentUserProvider>()
    private val clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val groupService = GroupService(
        groupRepository = groupRepository,
        userRepository = userRepository,
        currentUserProvider = currentUserProvider,
        clock = clock,
    )

    // ─── removeMember ─────────────────────────────────────────────────────────

    @Test
    fun `removeMember should throw ForbiddenException when non-owner tries to remove a member`() {
        val userId = UUID.randomUUID()
        val targetId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { groupRepository.isOwner(groupId, userId) } returns false

        assertThrows(ForbiddenException::class.java) {
            groupService.removeMember(groupId, targetId)
        }
    }

    @Test
    fun `removeMember should throw ForbiddenException when owner tries to remove themselves`() {
        val ownerId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(ownerId, "owner@example.com")
        every { groupRepository.isOwner(groupId, ownerId) } returns true

        assertThrows(ForbiddenException::class.java) {
            groupService.removeMember(groupId, ownerId)
        }
    }

    // ─── addMember ────────────────────────────────────────────────────────────

    @Test
    fun `addMember should throw ForbiddenException when caller is not the group owner`() {
        val userId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { groupRepository.isOwner(groupId, userId) } returns false

        assertThrows(ForbiddenException::class.java) {
            groupService.addMember(groupId, AddGroupMemberRequest(userId = UUID.randomUUID()))
        }
    }

    @Test
    fun `addMember should throw NotFoundException when user to add does not exist`() {
        val ownerId = UUID.randomUUID()
        val newMemberId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(ownerId, "owner@example.com")
        every { groupRepository.isOwner(groupId, ownerId) } returns true
        every { userRepository.existsById(newMemberId) } returns false

        assertThrows(NotFoundException::class.java) {
            groupService.addMember(groupId, AddGroupMemberRequest(userId = newMemberId))
        }
    }

    // ─── getGroup ─────────────────────────────────────────────────────────────

    @Test
    fun `getGroup should throw ForbiddenException when user is not a group member`() {
        val userId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(userId, "user@example.com")
        every { groupRepository.isMember(groupId, userId) } returns false

        assertThrows(ForbiddenException::class.java) {
            groupService.getGroup(groupId)
        }
    }

    // ─── createGroup ──────────────────────────────────────────────────────────

    @Test
    fun `createGroup should throw NotFoundException when a specified member does not exist`() {
        val ownerId = UUID.randomUUID()
        val unknownMemberId = UUID.randomUUID()
        every { currentUserProvider.requireCurrentUser() } returns AuthenticatedUser(ownerId, "owner@example.com")
        every { userRepository.existsById(ownerId) } returns true
        every { userRepository.existsById(unknownMemberId) } returns false

        assertThrows(NotFoundException::class.java) {
            groupService.createGroup(CreateGroupRequest(name = "Team", memberIds = setOf(unknownMemberId)))
        }
    }
}
