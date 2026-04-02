package org.misterstorm.eventplatform.todoapp.auth.domain

import java.time.Instant
import java.util.UUID

data class UserRecord(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val createdAt: Instant,
)

data class UserResponse(
    val id: UUID,
    val email: String,
    val displayName: String,
    val createdAt: Instant,
)

data class UserLookupResponse(
    val id: UUID,
    val displayName: String,
)

data class TokenResponse(
    val accessToken: String,
    val expiresAt: Instant,
    val user: UserResponse,
)

fun UserRecord.toResponse() = UserResponse(
    id = id,
    email = email,
    displayName = displayName,
    createdAt = createdAt,
)

fun UserRecord.toLookupResponse() = UserLookupResponse(
    id = id,
    displayName = displayName,
)

