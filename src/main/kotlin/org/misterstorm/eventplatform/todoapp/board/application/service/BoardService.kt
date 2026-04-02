package org.misterstorm.eventplatform.todoapp.board.application.service

import org.misterstorm.eventplatform.todoapp.board.adapters.out.persistence.BoardRepository
import org.misterstorm.eventplatform.todoapp.board.adapters.out.persistence.BoardStatusRepository
import org.misterstorm.eventplatform.todoapp.board.domain.BoardDetailsResponse
import org.misterstorm.eventplatform.todoapp.board.domain.BoardMemberResponse
import org.misterstorm.eventplatform.todoapp.board.domain.BoardRecord
import org.misterstorm.eventplatform.todoapp.board.domain.BoardResponse
import org.misterstorm.eventplatform.todoapp.board.domain.BoardStatusRecord
import org.misterstorm.eventplatform.todoapp.board.domain.BoardStatusResponse
import org.misterstorm.eventplatform.todoapp.board.domain.BoardTransitionRecord
import org.misterstorm.eventplatform.todoapp.board.domain.BoardTransitionResponse
import org.misterstorm.eventplatform.todoapp.board.domain.StatusKind
import org.misterstorm.eventplatform.todoapp.board.domain.toResponse
import org.misterstorm.eventplatform.todoapp.board.entrypoint.http.CreateBoardRequest
import org.misterstorm.eventplatform.todoapp.board.entrypoint.http.CreateBoardStatusRequest
import org.misterstorm.eventplatform.todoapp.board.entrypoint.http.ReplaceTransitionsRequest
import org.misterstorm.eventplatform.todoapp.common.error.domain.ConflictException
import org.misterstorm.eventplatform.todoapp.common.error.domain.ForbiddenException
import org.misterstorm.eventplatform.todoapp.common.error.domain.NotFoundException
import org.misterstorm.eventplatform.todoapp.common.logging.LoggingContextManager
import org.misterstorm.eventplatform.todoapp.common.security.service.CurrentUserProvider
import org.misterstorm.eventplatform.todoapp.group.adapters.out.persistence.GroupRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class BoardService(
    private val boardRepository: BoardRepository,
    private val boardStatusRepository: BoardStatusRepository,
    private val groupRepository: GroupRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val loggingContextManager: LoggingContextManager,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createBoard(groupId: UUID, request: CreateBoardRequest): BoardDetailsResponse {
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureGroupMember(groupId, currentUser.userId)

        val board = BoardRecord(
            id = UUID.randomUUID(),
            groupId = groupId,
            name = request.name.trim(),
            description = request.description?.trim()?.takeIf { it.isNotBlank() },
            createdBy = currentUser.userId,
            createdAt = Instant.now(clock),
        )
        boardRepository.insert(board)

        loggingContextManager.withBoard(board.id) {
            val todo = createSystemStatus(board.id, "TODO", "To Do", rank = 10, initial = true, terminal = false)
            val blocked = createSystemStatus(board.id, "BLOCKED", "Blocked", rank = 15, initial = false, terminal = false)
            val doing = createSystemStatus(board.id, "DOING", "Doing", rank = 20, initial = false, terminal = false)
            val done = createSystemStatus(board.id, "DONE", "Done", rank = 30, initial = false, terminal = true)
            boardStatusRepository.replaceTransitions(
                board.id,
                listOf(
                    BoardTransitionRecord(board.id, todo.id, doing.id),
                    BoardTransitionRecord(board.id, todo.id, blocked.id),
                    BoardTransitionRecord(board.id, doing.id, todo.id),
                    BoardTransitionRecord(board.id, doing.id, blocked.id),
                    BoardTransitionRecord(board.id, doing.id, done.id),
                    BoardTransitionRecord(board.id, blocked.id, todo.id),
                    BoardTransitionRecord(board.id, blocked.id, doing.id),
                    BoardTransitionRecord(board.id, done.id, doing.id),
                ),
            )
            logger.info("Board criado com statuses padrao e transicoes iniciais")
        }
        return getBoard(board.id)
    }

    fun listBoards(groupId: UUID): List<BoardResponse> {
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureGroupMember(groupId, currentUser.userId)
        return boardRepository.listByGroup(groupId).map { it.toResponse() }
    }

    fun getBoard(boardId: UUID): BoardDetailsResponse {
        val board = findBoardOrThrow(boardId)
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureGroupMember(board.groupId, currentUser.userId)
        return loggingContextManager.withBoard(boardId) {
            BoardDetailsResponse(
                id = board.id,
                groupId = board.groupId,
                name = board.name,
                description = board.description,
                createdBy = board.createdBy,
                createdAt = board.createdAt,
                members = groupRepository.listMembers(board.groupId).map {
                    BoardMemberResponse(
                        id = it.userId,
                        userId = it.userId,
                        displayName = it.displayName ?: "",
                        role = it.role,
                    )
                },
                statuses = boardStatusRepository.listByBoard(board.id).map { it.toResponse() },
                transitions = boardStatusRepository.listTransitions(board.id).map {
                    BoardTransitionResponse(it.fromStatusId, it.toStatusId)
                },
            )
        }
    }

    @Transactional
    fun createStatus(boardId: UUID, request: CreateBoardStatusRequest): BoardStatusResponse {
        val board = findBoardOrThrow(boardId)
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureGroupMember(board.groupId, currentUser.userId)
        return loggingContextManager.withBoard(boardId) {
            val code = request.code.trim().uppercase()
            if (boardStatusRepository.existsByCode(boardId, code)) {
                throw ConflictException("Ja existe um status com este codigo no board")
            }
            if (request.initial) {
                boardStatusRepository.clearInitialFlag(boardId)
            }

            val status = BoardStatusRecord(
                id = UUID.randomUUID(),
                boardId = boardId,
                code = code,
                name = request.name.trim(),
                kind = StatusKind.CUSTOM,
                rank = request.rank,
                isInitial = request.initial,
                isTerminal = request.terminal,
                createdAt = Instant.now(clock),
            )
            boardStatusRepository.insertStatus(status)
            logger.info("Novo status customizado criado no board")
            status.toResponse()
        }
    }

    @Transactional
    fun replaceTransitions(boardId: UUID, request: ReplaceTransitionsRequest): List<BoardTransitionResponse> {
        val board = findBoardOrThrow(boardId)
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureGroupMember(board.groupId, currentUser.userId)
        return loggingContextManager.withBoard(boardId) {
            val statuses = boardStatusRepository.listByBoard(boardId).associateBy { it.id }
            val transitions = request.transitions.map {
                val from = statuses[it.fromStatusId] ?: throw NotFoundException("Status de origem nao encontrado no board")
                val to = statuses[it.toStatusId] ?: throw NotFoundException("Status de destino nao encontrado no board")
                BoardTransitionRecord(boardId = boardId, fromStatusId = from.id, toStatusId = to.id)
            }
            boardStatusRepository.replaceTransitions(boardId, transitions)
            logger.info("Regras de transicao do board substituidas")
            transitions.map { BoardTransitionResponse(it.fromStatusId, it.toStatusId) }
        }
    }

    @Transactional
    fun deleteStatus(boardId: UUID, statusId: UUID) {
        val board = findBoardOrThrow(boardId)
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureGroupMember(board.groupId, currentUser.userId)
        loggingContextManager.withBoard(boardId) {
            val status = boardStatusRepository.findById(statusId)
                ?.takeIf { it.boardId == boardId }
                ?: throw NotFoundException("Status nao encontrado")
            if (status.kind == StatusKind.SYSTEM) {
                throw ForbiddenException("Statuses padrao do sistema nao podem ser removidos")
            }
            if (status.isInitial) {
                throw ForbiddenException("Status inicial nao pode ser removido")
            }
            if (boardStatusRepository.countTasksUsingStatus(statusId) > 0) {
                throw ConflictException("Nao e possivel remover um status em uso por tasks")
            }
            boardStatusRepository.deleteStatus(statusId)
            logger.info("Status removido do board")
        }
    }

    @Transactional
    fun deleteBoard(boardId: UUID) {
        val board = findBoardOrThrow(boardId)
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureGroupMember(board.groupId, currentUser.userId)
        loggingContextManager.withBoard(boardId) {
            boardRepository.deleteById(boardId)
            logger.info("Board removido")
        }
    }

    fun findBoardOrThrow(boardId: UUID): BoardRecord = boardRepository.findById(boardId)
        ?: throw NotFoundException("Board nao encontrado")

    fun findStatusOrThrow(statusId: UUID): BoardStatusRecord = boardStatusRepository.findById(statusId)
        ?: throw NotFoundException("Status nao encontrado")

    fun findInitialStatus(boardId: UUID): BoardStatusRecord = boardStatusRepository.findInitialByBoard(boardId)
        ?: throw NotFoundException("Board sem status inicial configurado")

    fun hasTransition(boardId: UUID, fromStatusId: UUID, toStatusId: UUID): Boolean =
        boardStatusRepository.hasTransition(boardId, fromStatusId, toStatusId)

    fun listStatuses(boardId: UUID): List<BoardStatusResponse> {
        val board = findBoardOrThrow(boardId)
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureGroupMember(board.groupId, currentUser.userId)
        return loggingContextManager.withBoard(boardId) {
            boardStatusRepository.listByBoard(boardId).map { it.toResponse() }
        }
    }

    fun listTransitions(boardId: UUID): List<BoardTransitionResponse> {
        val board = findBoardOrThrow(boardId)
        val currentUser = currentUserProvider.requireCurrentUser()
        ensureGroupMember(board.groupId, currentUser.userId)
        return loggingContextManager.withBoard(boardId) {
            boardStatusRepository.listTransitions(boardId).map { BoardTransitionResponse(it.fromStatusId, it.toStatusId) }
        }
    }

    private fun ensureGroupMember(groupId: UUID, userId: UUID) {
        if (!groupRepository.isMember(groupId, userId)) {
            throw ForbiddenException("Usuario nao pertence ao grupo do board")
        }
    }

    private fun createSystemStatus(
        boardId: UUID,
        code: String,
        name: String,
        rank: Int,
        initial: Boolean,
        terminal: Boolean,
    ): BoardStatusRecord {
        val status = BoardStatusRecord(
            id = UUID.randomUUID(),
            boardId = boardId,
            code = code,
            name = name,
            kind = StatusKind.SYSTEM,
            rank = rank,
            isInitial = initial,
            isTerminal = terminal,
            createdAt = Instant.now(clock),
        )
        boardStatusRepository.insertStatus(status)
        return status
    }
}
