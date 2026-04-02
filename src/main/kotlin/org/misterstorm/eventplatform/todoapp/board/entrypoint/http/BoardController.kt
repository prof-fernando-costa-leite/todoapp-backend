package org.misterstorm.eventplatform.todoapp.board.entrypoint.http

import jakarta.validation.Valid
import org.misterstorm.eventplatform.todoapp.board.application.service.BoardService
import org.misterstorm.eventplatform.todoapp.board.domain.BoardDetailsResponse
import org.misterstorm.eventplatform.todoapp.board.domain.BoardResponse
import org.misterstorm.eventplatform.todoapp.board.domain.BoardStatusResponse
import org.misterstorm.eventplatform.todoapp.board.domain.BoardTransitionResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class BoardController(
    private val boardService: BoardService,
) {

    @PostMapping("/groups/{groupId}/boards")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBoard(
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: CreateBoardRequest,
    ): BoardDetailsResponse = boardService.createBoard(groupId, request)

    @GetMapping("/groups/{groupId}/boards")
    fun listBoards(@PathVariable groupId: UUID): List<BoardResponse> = boardService.listBoards(groupId)

    @GetMapping("/boards/{boardId}")
    fun getBoard(@PathVariable boardId: UUID): BoardDetailsResponse = boardService.getBoard(boardId)

    @GetMapping("/boards/{boardId}/statuses")
    fun listStatuses(@PathVariable boardId: UUID): List<BoardStatusResponse> = boardService.listStatuses(boardId)

    @GetMapping("/boards/{boardId}/transitions")
    fun listTransitions(@PathVariable boardId: UUID): List<BoardTransitionResponse> = boardService.listTransitions(boardId)

    @PostMapping("/boards/{boardId}/statuses")
    @ResponseStatus(HttpStatus.CREATED)
    fun createStatus(
        @PathVariable boardId: UUID,
        @Valid @RequestBody request: CreateBoardStatusRequest,
    ): BoardStatusResponse = boardService.createStatus(boardId, request)

    @PutMapping("/boards/{boardId}/transitions")
    fun replaceTransitions(
        @PathVariable boardId: UUID,
        @Valid @RequestBody request: ReplaceTransitionsRequest,
    ): List<BoardTransitionResponse> = boardService.replaceTransitions(boardId, request)

    @DeleteMapping("/boards/{boardId}/statuses/{statusId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteStatus(
        @PathVariable boardId: UUID,
        @PathVariable statusId: UUID,
    ) {
        boardService.deleteStatus(boardId, statusId)
    }

    @DeleteMapping("/boards/{boardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteBoard(@PathVariable boardId: UUID) {
        boardService.deleteBoard(boardId)
    }
}
