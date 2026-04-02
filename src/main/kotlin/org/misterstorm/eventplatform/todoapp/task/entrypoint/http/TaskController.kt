package org.misterstorm.eventplatform.todoapp.task.entrypoint.http

import jakarta.validation.Valid
import org.misterstorm.eventplatform.todoapp.task.application.service.TaskService
import org.misterstorm.eventplatform.todoapp.task.domain.TaskHistoryResponse
import org.misterstorm.eventplatform.todoapp.task.domain.TaskResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class TaskController(
    private val taskService: TaskService,
) {

    @PostMapping("/boards/{boardId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    fun createTask(
        @PathVariable boardId: UUID,
        @Valid @RequestBody request: CreateTaskRequest,
    ): TaskResponse = taskService.createTask(boardId, request)

    @GetMapping("/boards/{boardId}/tasks")
    fun listBoardTasks(@PathVariable boardId: UUID): List<TaskResponse> = taskService.listBoardTasks(boardId)

    @GetMapping("/tasks/mine")
    fun listMyTasks(): List<TaskResponse> = taskService.listMyTasks()

    @GetMapping("/tasks/{taskId}")
    fun getTask(@PathVariable taskId: UUID): TaskResponse = taskService.getTask(taskId)

    @PatchMapping("/tasks/{taskId}")
    fun updateTask(
        @PathVariable taskId: UUID,
        @Valid @RequestBody request: UpdateTaskRequest,
    ): TaskResponse = taskService.updateTask(taskId, request)

    @PatchMapping("/tasks/{taskId}/status")
    fun changeStatus(
        @PathVariable taskId: UUID,
        @Valid @RequestBody request: ChangeTaskStatusRequest,
    ): TaskResponse = taskService.changeStatus(taskId, request)

    @PatchMapping("/tasks/{taskId}/assignee")
    fun updateAssignee(
        @PathVariable taskId: UUID,
        @Valid @RequestBody request: UpdateTaskAssigneeRequest,
    ): TaskResponse = taskService.updateAssignee(taskId, request)

    @PatchMapping("/tasks/{taskId}/blocker")
    fun updateBlocker(
        @PathVariable taskId: UUID,
        @Valid @RequestBody request: UpdateTaskBlockerRequest,
    ): TaskResponse = taskService.updateBlocker(taskId, request)

    @GetMapping("/tasks/{taskId}/history")
    fun getHistory(@PathVariable taskId: UUID): List<TaskHistoryResponse> = taskService.getHistory(taskId)

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTask(@PathVariable taskId: UUID) {
        taskService.deleteTask(taskId)
    }
}
