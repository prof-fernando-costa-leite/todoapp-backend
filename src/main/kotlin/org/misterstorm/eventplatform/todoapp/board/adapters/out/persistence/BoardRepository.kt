package org.misterstorm.eventplatform.todoapp.board.adapters.out.persistence

import org.misterstorm.eventplatform.todoapp.board.domain.BoardRecord
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.Date
import java.util.UUID

@Repository
class BoardRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    private val boardRowMapper = RowMapper<BoardRecord> { rs, _ ->
        BoardRecord(
            id = rs.getObject("id", UUID::class.java),
            groupId = rs.getObject("group_id", UUID::class.java),
            name = rs.getString("name"),
            description = rs.getString("description"),
            createdBy = rs.getObject("created_by", UUID::class.java),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }

    fun insert(board: BoardRecord) {
        jdbcTemplate.update(
            """
            insert into boards (id, group_id, name, description, created_by, created_at)
            values (:id, :groupId, :name, :description, :createdBy, :createdAt)
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", board.id)
                .addValue("groupId", board.groupId)
                .addValue("name", board.name)
                .addValue("description", board.description)
                .addValue("createdBy", board.createdBy)
                .addValue("createdAt", Date.from(board.createdAt)),
        )
    }

    fun findById(boardId: UUID): BoardRecord? = jdbcTemplate.query(
        "select id, group_id, name, description, created_by, created_at from boards where id = :boardId",
        mapOf("boardId" to boardId),
        boardRowMapper,
    ).firstOrNull()

    fun listByGroup(groupId: UUID): List<BoardRecord> = jdbcTemplate.query(
        """
        select id, group_id, name, description, created_by, created_at
        from boards
        where group_id = :groupId
        order by created_at desc
        """.trimIndent(),
        mapOf("groupId" to groupId),
        boardRowMapper,
    )

    fun deleteById(boardId: UUID): Int = jdbcTemplate.update(
        "delete from boards where id = :boardId",
        mapOf("boardId" to boardId),
    )
}
