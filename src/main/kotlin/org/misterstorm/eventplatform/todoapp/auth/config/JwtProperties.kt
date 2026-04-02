package org.misterstorm.eventplatform.todoapp.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.security.jwt")
data class JwtProperties(
    val issuer: String,
    val secret: String,
    val expiration: Duration,
)

