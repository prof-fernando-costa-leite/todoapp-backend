package org.misterstorm.eventplatform.todoapp.common.security.config

import org.slf4j.MDC
import org.springframework.http.HttpStatus
import java.time.Instant

fun errorResponseJson(status: HttpStatus, message: String, path: String): String {
    val actionId = MDC.get("actionId")?.let { "\"$it\"" } ?: "null"
    return """
        {"timestamp":"${Instant.now()}","status":${status.value()},"error":"${status.reasonPhrase}","message":"$message","path":"$path","actionId":$actionId}
    """.trimIndent()
}

