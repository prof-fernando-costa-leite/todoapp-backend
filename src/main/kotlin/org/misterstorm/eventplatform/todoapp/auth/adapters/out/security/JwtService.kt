package org.misterstorm.eventplatform.todoapp.auth.adapters.out.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.misterstorm.eventplatform.todoapp.auth.application.port.out.JwtTokenPort
import org.misterstorm.eventplatform.todoapp.auth.config.JwtProperties
import org.misterstorm.eventplatform.todoapp.auth.domain.TokenResponse
import org.misterstorm.eventplatform.todoapp.auth.domain.UserRecord
import org.misterstorm.eventplatform.todoapp.auth.domain.toResponse
import org.misterstorm.eventplatform.todoapp.common.security.model.AuthenticatedUser
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    private val jwtProperties: JwtProperties,
    private val clock: Clock,
) : JwtTokenPort {
    private val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret))

    override fun createToken(user: UserRecord): TokenResponse {
        val issuedAt = Instant.now(clock)
        val expiresAt = issuedAt.plus(jwtProperties.expiration)
        val token = Jwts.builder()
            .subject(user.id.toString())
            .issuer(jwtProperties.issuer)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .claim("email", user.email)
            .claim("displayName", user.displayName)
            .signWith(key)
            .compact()

        return TokenResponse(
            accessToken = token,
            expiresAt = expiresAt,
            user = user.toResponse(),
        )
    }

    override fun parse(token: String): AuthenticatedUser {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        return AuthenticatedUser(
            userId = UUID.fromString(claims.subject),
            email = claims.get("email", String::class.java),
        )
    }
}

