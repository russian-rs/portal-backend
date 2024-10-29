package rs.russian.portal.shared.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import rs.russian.generated.model.ErrorResponse

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        var message = ex.message ?: "Unknown error"
        ex.cause?.message?.let { message = it }
        return ResponseEntity(ErrorResponse(message), HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
