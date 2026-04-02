package org.misterstorm.eventplatform.todoapp.auth.adapters.out.persistence

import org.misterstorm.eventplatform.todoapp.auth.application.port.out.UserRepositoryPort
import org.misterstorm.eventplatform.todoapp.auth.domain.UserRecord
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.Date
import java.util.UUID

@Repository
class JdbcUserRepositoryAdapter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : UserRepositoryPort {
    private val rowMapper = RowMapper<UserRecord> { rs, _ ->
        UserRecord(
            id = rs.getObject("id", UUID::class.java),
            email = rs.getString("email"),
            passwordHash = rs.getString("password_hash"),
            displayName = rs.getString("display_name"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }

    override fun findByEmail(email: String): UserRecord? = jdbcTemplate.query(
        """
        select id, email, password_hash, display_name, created_at
        from app_users
        where email = :email
        """.trimIndent(),
        mapOf("email" to email),
        rowMapper,
    ).firstOrNull()

    override fun findById(id: UUID): UserRecord? = jdbcTemplate.query(
        """
        select id, email, password_hash, display_name, created_at
        from app_users
        where id = :id
        """.trimIndent(),
        mapOf("id" to id),
        rowMapper,
    ).firstOrNull()

    override fun existsById(id: UUID): Boolean = jdbcTemplate.queryForObject(
        "select exists(select 1 from app_users where id = :id)",
        mapOf("id" to id),
        Boolean::class.java,
    ) ?: false

    override fun listAll(): List<UserRecord> = jdbcTemplate.query(
        """
        select id, email, password_hash, display_name, created_at
        from app_users
        order by display_name asc, id asc
        """.trimIndent(),
        emptyMap<String, Any>(),
        rowMapper,
    )

    override fun insert(user: UserRecord) {
        jdbcTemplate.update(
            """
            insert into app_users (id, email, password_hash, display_name, created_at)
            values (:id, :email, :passwordHash, :displayName, :createdAt)
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", user.id)
                .addValue("email", user.email)
                .addValue("passwordHash", user.passwordHash)
                .addValue("displayName", user.displayName)
                .addValue("createdAt", Date.from(user.createdAt)),
        )
    }
}

