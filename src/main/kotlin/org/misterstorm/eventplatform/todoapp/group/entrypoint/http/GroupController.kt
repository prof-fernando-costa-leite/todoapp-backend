package org.misterstorm.eventplatform.todoapp.group.entrypoint.http

import jakarta.validation.Valid
import org.misterstorm.eventplatform.todoapp.group.application.service.GroupService
import org.misterstorm.eventplatform.todoapp.group.domain.GroupDetailsResponse
import org.misterstorm.eventplatform.todoapp.group.domain.GroupSummaryResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/groups")
class GroupController(
    private val groupService: GroupService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createGroup(@Valid @RequestBody request: CreateGroupRequest): GroupDetailsResponse = groupService.createGroup(request)

    @GetMapping
    fun listGroups(): List<GroupSummaryResponse> = groupService.listGroups()

    @GetMapping("/{groupId}")
    fun getGroup(@PathVariable groupId: UUID): GroupDetailsResponse = groupService.getGroup(groupId)

    @PostMapping("/{groupId}/members")
    fun addMember(
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: AddGroupMemberRequest,
    ): GroupDetailsResponse = groupService.addMember(groupId, request)

    @DeleteMapping("/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeMember(
        @PathVariable groupId: UUID,
        @PathVariable userId: UUID,
    ) {
        groupService.removeMember(groupId, userId)
    }
}
