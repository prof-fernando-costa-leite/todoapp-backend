package org.misterstorm.eventplatform.todoapp.task.adapters.out.persistence

import org.misterstorm.eventplatform.todoapp.task.domain.TaskDetails
import org.misterstorm.eventplatform.todoapp.task.domain.TaskHistoryEntry
import org.misterstorm.eventplatform.todoapp.task.domain.TaskPriority
import org.misterstorm.eventplatform.todoapp.task.domain.TaskRecord
import org.misterstorm.eventplatform.todoapp.task.domain.TaskStatusHistoryRecord
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Date
import java.util.UUID

@Repository
class TaskRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    private val taskRowMapper = RowMapper<TaskDetails> { rs, _ ->
        TaskDetails(
            id = rs.getObject("id", UUID::class.java),
            boardId = rs.getObject("board_id", UUID::class.java),
            groupId = rs.getObject("group_id", UUID::class.java),
            creatorId = rs.getObject("creator_id", UUID::class.java),
            creatorDisplayName = rs.getString("creator_display_name"),
            assigneeId = rs.getObject("assignee_id", UUID::class.java),
            assigneeDisplayName = rs.getString("assignee_display_name"),
            title = rs.getString("title"),
            description = rs.getString("description"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant(),
            statusId = rs.getObject("status_id", UUID::class.java),
            statusCode = rs.getString("status_code"),
            statusName = rs.getString("status_name"),
            statusRank = rs.getInt("status_rank"),
            statusTerminal = rs.getBoolean("status_terminal"),
            points = rs.getObject("points") as Int?,
            priority = TaskPriority.valueOf(rs.getString("priority")),
            blockingTaskId = rs.getObject("blocking_task_id", UUID::class.java),
        )
    }

    private val historyRowMapper = RowMapper<TaskHistoryEntry> { rs, _ ->
        TaskHistoryEntry(
            id = rs.getObject("id", UUID::class.java),
            taskId = rs.getObject("task_id", UUID::class.java),
            fromStatusId = rs.getObject("from_status_id", UUID::class.java),
            fromStatusName = rs.getString("from_status_name"),
            toStatusId = rs.getObject("to_status_id", UUID::class.java),
            toStatusName = rs.getString("to_status_name"),
            changedBy = rs.getObject("changed_by", UUID::class.java),
            changedAt = rs.getTimestamp("changed_at").toInstant(),
        )
    }

    fun insert(task: TaskRecord) {
        jdbcTemplate.update(
            """
            insert into tasks (
                id, board_id, creator_id, assignee_id, title, description,
                created_at, updated_at, status_id, points, priority, blocking_task_id
            ) values (
                :id, :boardId, :creatorId, :assigneeId, :title, :description,
                :createdAt, :updatedAt, :statusId, :points, :priority, :blockingTaskId
            )
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", task.id)
                .addValue("boardId", task.boardId)
                .addValue("creatorId", task.creatorId)
                .addValue("assigneeId", task.assigneeId)
                .addValue("title", task.title)
                .addValue("description", task.description)
                .addValue("createdAt", Date.from(task.createdAt))
                .addValue("updatedAt", Date.from(task.updatedAt))
                .addValue("statusId", task.statusId)
                .addValue("points", task.points)
                .addValue("priority", task.priority.name)
                .addValue("blockingTaskId", task.blockingTaskId),
        )
    }

    fun insertHistory(history: TaskStatusHistoryRecord) {
        jdbcTemplate.update(
            """
            insert into task_status_history (id, task_id, from_status_id, to_status_id, changed_by, changed_at)
            values (:id, :taskId, :fromStatusId, :toStatusId, :changedBy, :changedAt)
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", history.id)
                .addValue("taskId", history.taskId)
                .addValue("fromStatusId", history.fromStatusId)
                .addValue("toStatusId", history.toStatusId)
                .addValue("changedBy", history.changedBy)
                .addValue("changedAt", Date.from(history.changedAt)),
        )
    }

    fun findById(taskId: UUID): TaskDetails? = jdbcTemplate.query(baseTaskQuery("where t.id = :taskId"), mapOf("taskId" to taskId), taskRowMapper).firstOrNull()

    fun listByBoard(boardId: UUID): List<TaskDetails> = jdbcTemplate.query(
        baseTaskQuery("where t.board_id = :boardId order by t.created_at desc"),
        mapOf("boardId" to boardId),
        taskRowMapper,
    )

    fun listMine(userId: UUID): List<TaskDetails> = jdbcTemplate.query(
        baseTaskQuery("where t.creator_id = :userId or t.assignee_id = :userId order by t.updated_at desc"),
        mapOf("userId" to userId),
        taskRowMapper,
    )

    fun updateTask(taskId: UUID, title: String?, description: String?, points: Int?, priority: TaskPriority?, updatedAt: Instant) {
        jdbcTemplate.update(
            """
            update tasks
            set title = :title,
                description = :description,
                points = :points,
                priority = :priority,
                updated_at = :updatedAt
            where id = :taskId
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("taskId", taskId)
                .addValue("title", title)
                .addValue("description", description)
                .addValue("points", points)
                .addValue("priority", priority?.name)
                .addValue("updatedAt", Date.from(updatedAt)),
        )
    }

    fun updateAssignee(taskId: UUID, assigneeId: UUID?, updatedAt: Instant) {
        jdbcTemplate.update(
            "update tasks set assignee_id = :assigneeId, updated_at = :updatedAt where id = :taskId",
            MapSqlParameterSource()
                .addValue("taskId", taskId)
                .addValue("assigneeId", assigneeId)
                .addValue("updatedAt", Date.from(updatedAt)),
        )
    }

    fun updateBlocker(taskId: UUID, blockerTaskId: UUID?, updatedAt: Instant) {
        jdbcTemplate.update(
            "update tasks set blocking_task_id = :blockingTaskId, updated_at = :updatedAt where id = :taskId",
            MapSqlParameterSource()
                .addValue("taskId", taskId)
                .addValue("blockingTaskId", blockerTaskId)
                .addValue("updatedAt", Date.from(updatedAt)),
        )
    }

    fun updateStatus(taskId: UUID, statusId: UUID, updatedAt: Instant) {
        jdbcTemplate.update(
            "update tasks set status_id = :statusId, updated_at = :updatedAt where id = :taskId",
            MapSqlParameterSource()
                .addValue("taskId", taskId)
                .addValue("statusId", statusId)
                .addValue("updatedAt", Date.from(updatedAt)),
        )
    }

    fun clearBlockerReferences(blockerTaskId: UUID, updatedAt: Instant) {
        jdbcTemplate.update(
            """
            update tasks
            set blocking_task_id = null,
                updated_at = :updatedAt
            where blocking_task_id = :blockerTaskId
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("blockerTaskId", blockerTaskId)
                .addValue("updatedAt", Date.from(updatedAt)),
        )
    }

    fun deleteById(taskId: UUID): Int = jdbcTemplate.update(
        "delete from tasks where id = :taskId",
        mapOf("taskId" to taskId),
    )

    fun listHistory(taskId: UUID): List<TaskHistoryEntry> = jdbcTemplate.query(
        """
        select h.id,
               h.task_id,
               h.from_status_id,
               fs.name as from_status_name,
               h.to_status_id,
               ts.name as to_status_name,
               h.changed_by,
               h.changed_at
        from task_status_history h
        left join board_statuses fs on fs.id = h.from_status_id
        inner join board_statuses ts on ts.id = h.to_status_id
        where h.task_id = :taskId
        order by h.changed_at asc
        """.trimIndent(),
        mapOf("taskId" to taskId),
        historyRowMapper,
    )

    private fun baseTaskQuery(whereClause: String): String =
        """
        select t.id,
               t.board_id,
               b.group_id,
               t.creator_id,
               creator.display_name as creator_display_name,
               t.assignee_id,
               assignee.display_name as assignee_display_name,
               t.title,
               t.description,
               t.created_at,
               t.updated_at,
               t.status_id,
               s.code as status_code,
               s.name as status_name,
               s.rank as status_rank,
               s.is_terminal as status_terminal,
               t.points,
               t.priority,
               t.blocking_task_id
        from tasks t
        inner join boards b on b.id = t.board_id
        inner join board_statuses s on s.id = t.status_id
        inner join app_users creator on creator.id = t.creator_id
        left join app_users assignee on assignee.id = t.assignee_id
        $whereClause
        """.trimIndent()
}

