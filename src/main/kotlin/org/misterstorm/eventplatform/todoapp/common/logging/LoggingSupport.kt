package org.misterstorm.eventplatform.todoapp.common.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component("actionContextFilter")
class RequestContextFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        MDC.put("actionId", UUID.randomUUID().toString())
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }
}

@Component
class LoggingContextManager {

    fun <T> withBoard(boardId: UUID, action: () -> T): T {
        val previousValue = MDC.get("boardId")
        MDC.put("boardId", boardId.toString())
        return try {
            action()
        } finally {
            if (previousValue == null) {
                MDC.remove("boardId")
            } else {
                MDC.put("boardId", previousValue)
            }
        }
    }
}


