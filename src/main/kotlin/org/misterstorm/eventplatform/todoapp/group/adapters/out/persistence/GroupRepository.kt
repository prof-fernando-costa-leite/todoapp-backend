package org.misterstorm.eventplatform.todoapp.group.adapters.out.persistence

import org.misterstorm.eventplatform.todoapp.group.domain.GroupMemberRecord
import org.misterstorm.eventplatform.todoapp.group.domain.GroupMemberRole
import org.misterstorm.eventplatform.todoapp.group.domain.GroupRecord
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.Date
import java.util.UUID

@Repository
class GroupRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    private val groupRowMapper = RowMapper<GroupRecord> { rs, _ ->
        GroupRecord(
            id = rs.getObject("id", UUID::class.java),
            name = rs.getString("name"),
            createdBy = rs.getObject("created_by", UUID::class.java),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }

    private val memberRowMapper = RowMapper<GroupMemberRecord> { rs, _ ->
        GroupMemberRecord(
            groupId = rs.getObject("group_id", UUID::class.java),
            userId = rs.getObject("user_id", UUID::class.java),
            role = GroupMemberRole.valueOf(rs.getString("member_role")),
            addedAt = rs.getTimestamp("added_at").toInstant(),
            displayName = rs.getString("display_name"),
        )
    }

    fun insertGroup(group: GroupRecord) {
        jdbcTemplate.update(
            """
            insert into app_groups (id, name, created_by, created_at)
            values (:id, :name, :createdBy, :createdAt)
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", group.id)
                .addValue("name", group.name)
                .addValue("createdBy", group.createdBy)
                .addValue("createdAt", Date.from(group.createdAt)),
        )
    }

    fun addMember(member: GroupMemberRecord) {
        jdbcTemplate.update(
            """
            insert into group_members (group_id, user_id, member_role, added_at)
            values (:groupId, :userId, :role, :addedAt)
            on conflict (group_id, user_id) do update set member_role = excluded.member_role
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("groupId", member.groupId)
                .addValue("userId", member.userId)
                .addValue("role", member.role.name)
                .addValue("addedAt", Date.from(member.addedAt)),
        )
    }

    fun findById(groupId: UUID): GroupRecord? = jdbcTemplate.query(
        "select id, name, created_by, created_at from app_groups where id = :groupId",
        mapOf("groupId" to groupId),
        groupRowMapper,
    ).firstOrNull()

    fun listByUser(userId: UUID): List<GroupRecord> = jdbcTemplate.query(
        """
        select g.id, g.name, g.created_by, g.created_at
        from app_groups g
        inner join group_members gm on gm.group_id = g.id
        where gm.user_id = :userId
        order by g.created_at desc
        """.trimIndent(),
        mapOf("userId" to userId),
        groupRowMapper,
    )

    fun listMembers(groupId: UUID): List<GroupMemberRecord> = jdbcTemplate.query(
        """
        select gm.group_id, gm.user_id, gm.member_role, gm.added_at, u.display_name
        from group_members gm
        inner join app_users u on u.id = gm.user_id
        where gm.group_id = :groupId
        order by gm.added_at asc
        """.trimIndent(),
        mapOf("groupId" to groupId),
        memberRowMapper,
    )

    fun isMember(groupId: UUID, userId: UUID): Boolean = jdbcTemplate.queryForObject(
        "select exists(select 1 from group_members where group_id = :groupId and user_id = :userId)",
        mapOf("groupId" to groupId, "userId" to userId),
        Boolean::class.java,
    ) ?: false

    fun isOwner(groupId: UUID, userId: UUID): Boolean = jdbcTemplate.queryForObject(
        """
        select exists(
            select 1 from group_members
            where group_id = :groupId and user_id = :userId and member_role = 'OWNER'
        )
        """.trimIndent(),
        mapOf("groupId" to groupId, "userId" to userId),
        Boolean::class.java,
    ) ?: false

    fun removeMember(groupId: UUID, userId: UUID) {
        jdbcTemplate.update(
            "delete from group_members where group_id = :groupId and user_id = :userId",
            mapOf("groupId" to groupId, "userId" to userId),
        )
    }
}
