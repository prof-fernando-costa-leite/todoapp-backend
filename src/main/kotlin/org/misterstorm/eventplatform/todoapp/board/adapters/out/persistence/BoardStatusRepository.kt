package org.misterstorm.eventplatform.todoapp.board.adapters.out.persistence

import org.misterstorm.eventplatform.todoapp.board.domain.BoardStatusRecord
import org.misterstorm.eventplatform.todoapp.board.domain.BoardTransitionRecord
import org.misterstorm.eventplatform.todoapp.board.domain.StatusKind
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.Date
import java.util.UUID

@Repository
class BoardStatusRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    private val statusRowMapper = RowMapper<BoardStatusRecord> { rs, _ ->
        BoardStatusRecord(
            id = rs.getObject("id", UUID::class.java),
            boardId = rs.getObject("board_id", UUID::class.java),
            code = rs.getString("code"),
            name = rs.getString("name"),
            kind = StatusKind.valueOf(rs.getString("status_kind")),
            rank = rs.getInt("rank"),
            isInitial = rs.getBoolean("is_initial"),
            isTerminal = rs.getBoolean("is_terminal"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }

    private val transitionRowMapper = RowMapper<BoardTransitionRecord> { rs, _ ->
        BoardTransitionRecord(
            boardId = rs.getObject("board_id", UUID::class.java),
            fromStatusId = rs.getObject("from_status_id", UUID::class.java),
            toStatusId = rs.getObject("to_status_id", UUID::class.java),
        )
    }

    fun insertStatus(status: BoardStatusRecord) {
        jdbcTemplate.update(
            """
            insert into board_statuses (id, board_id, code, name, status_kind, rank, is_initial, is_terminal, created_at)
            values (:id, :boardId, :code, :name, :kind, :rank, :isInitial, :isTerminal, :createdAt)
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", status.id)
                .addValue("boardId", status.boardId)
                .addValue("code", status.code)
                .addValue("name", status.name)
                .addValue("kind", status.kind.name)
                .addValue("rank", status.rank)
                .addValue("isInitial", status.isInitial)
                .addValue("isTerminal", status.isTerminal)
                .addValue("createdAt", Date.from(status.createdAt)),
        )
    }

    fun clearInitialFlag(boardId: UUID) {
        jdbcTemplate.update(
            "update board_statuses set is_initial = false where board_id = :boardId",
            mapOf("boardId" to boardId),
        )
    }

    fun listByBoard(boardId: UUID): List<BoardStatusRecord> = jdbcTemplate.query(
        """
        select id, board_id, code, name, status_kind, rank, is_initial, is_terminal, created_at
        from board_statuses
        where board_id = :boardId
        order by rank asc, created_at asc
        """.trimIndent(),
        mapOf("boardId" to boardId),
        statusRowMapper,
    )

    fun findById(statusId: UUID): BoardStatusRecord? = jdbcTemplate.query(
        """
        select id, board_id, code, name, status_kind, rank, is_initial, is_terminal, created_at
        from board_statuses
        where id = :statusId
        """.trimIndent(),
        mapOf("statusId" to statusId),
        statusRowMapper,
    ).firstOrNull()

    fun findInitialByBoard(boardId: UUID): BoardStatusRecord? = jdbcTemplate.query(
        """
        select id, board_id, code, name, status_kind, rank, is_initial, is_terminal, created_at
        from board_statuses
        where board_id = :boardId and is_initial = true
        order by created_at asc
        limit 1
        """.trimIndent(),
        mapOf("boardId" to boardId),
        statusRowMapper,
    ).firstOrNull()

    fun existsByCode(boardId: UUID, code: String): Boolean = jdbcTemplate.queryForObject(
        "select exists(select 1 from board_statuses where board_id = :boardId and code = :code)",
        mapOf("boardId" to boardId, "code" to code),
        Boolean::class.java,
    ) ?: false

    fun insertTransition(transition: BoardTransitionRecord) {
        jdbcTemplate.update(
            """
            insert into board_status_transitions (board_id, from_status_id, to_status_id)
            values (:boardId, :fromStatusId, :toStatusId)
            on conflict do nothing
            """.trimIndent(),
            mapOf(
                "boardId" to transition.boardId,
                "fromStatusId" to transition.fromStatusId,
                "toStatusId" to transition.toStatusId,
            ),
        )
    }

    fun replaceTransitions(boardId: UUID, transitions: List<BoardTransitionRecord>) {
        jdbcTemplate.update(
            "delete from board_status_transitions where board_id = :boardId",
            mapOf("boardId" to boardId),
        )
        transitions.forEach(::insertTransition)
    }

    fun listTransitions(boardId: UUID): List<BoardTransitionRecord> = jdbcTemplate.query(
        """
        select board_id, from_status_id, to_status_id
        from board_status_transitions
        where board_id = :boardId
        order by from_status_id, to_status_id
        """.trimIndent(),
        mapOf("boardId" to boardId),
        transitionRowMapper,
    )

    fun hasTransition(boardId: UUID, fromStatusId: UUID, toStatusId: UUID): Boolean = jdbcTemplate.queryForObject(
        """
        select exists(
            select 1 from board_status_transitions
            where board_id = :boardId and from_status_id = :fromStatusId and to_status_id = :toStatusId
        )
        """.trimIndent(),
        mapOf("boardId" to boardId, "fromStatusId" to fromStatusId, "toStatusId" to toStatusId),
        Boolean::class.java,
    ) ?: false

    fun countTasksUsingStatus(statusId: UUID): Int = jdbcTemplate.queryForObject(
        "select count(*) from tasks where status_id = :statusId",
        mapOf("statusId" to statusId),
        Int::class.java,
    ) ?: 0

    fun deleteStatus(statusId: UUID) {
        jdbcTemplate.update(
            "delete from board_statuses where id = :statusId",
            mapOf("statusId" to statusId),
        )
    }
}
