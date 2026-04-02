package org.misterstorm.eventplatform.todoapp.common.security.entrypoint

import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.misterstorm.eventplatform.todoapp.auth.application.port.out.JwtTokenPort
import org.misterstorm.eventplatform.todoapp.common.security.config.errorResponseJson
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenPort: JwtTokenPort,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.method == HttpMethod.OPTIONS.name()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authorizationHeader.isNullOrBlank() || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authorizationHeader.removePrefix("Bearer ").trim()

        try {
            val user = jwtTokenPort.parse(token)
            val authentication: Authentication = UsernamePasswordAuthenticationToken(user, token, emptyList())
            SecurityContextHolder.getContext().authentication = authentication
            MDC.put("userId", user.userId.toString())
            filterChain.doFilter(request, response)
        } catch (_: JwtException) {
            writeUnauthorizedResponse(response, request.requestURI)
        } catch (_: IllegalArgumentException) {
            writeUnauthorizedResponse(response, request.requestURI)
        } finally {
            SecurityContextHolder.clearContext()
            MDC.remove("userId")
        }
    }

    private fun writeUnauthorizedResponse(response: HttpServletResponse, path: String) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(errorResponseJson(HttpStatus.UNAUTHORIZED, "Token JWT invalido ou expirado", path))
    }
}
