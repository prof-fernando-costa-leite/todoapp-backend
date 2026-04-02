package org.misterstorm.eventplatform.todoapp.common.error.entrypoint

import jakarta.servlet.http.HttpServletRequest
import org.misterstorm.eventplatform.todoapp.common.error.domain.ApiException
import org.misterstorm.eventplatform.todoapp.common.error.domain.ErrorResponse
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.converter.HttpMessageNotReadableException
import java.time.Instant

@RestControllerAdvice
class RestExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        exception: ApiException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = buildResponse(exception.status, exception.message, request)

    @ExceptionHandler(MethodArgumentNotValidException::class, BindException::class)
    fun handleValidationException(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val message = when (exception) {
            is MethodArgumentNotValidException -> exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            is BindException -> exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            else -> null
        } ?: "Invalid input data"

        return buildResponse(HttpStatus.BAD_REQUEST, message, request)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val message = if (exception.message?.contains("statusId", ignoreCase = true) == true) {
            "statusId is required"
        } else {
            "Invalid request body"
        }
        return buildResponse(HttpStatus.BAD_REQUEST, message, request)
    }

    @ExceptionHandler(Exception::class)
    @Suppress("UNUSED_PARAMETER")
    fun handleUnexpectedException(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Unexpected internal error",
        request,
    )

    private fun buildResponse(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = ResponseEntity.status(status).body(
        ErrorResponse(
            timestamp = Instant.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.requestURI,
            actionId = MDC.get("actionId"),
        ),
    )
}
