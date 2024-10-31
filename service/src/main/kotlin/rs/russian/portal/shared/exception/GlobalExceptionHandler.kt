package rs.russian.portal.shared.exception

import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import rs.russian.generated.model.ErrorResponse

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(INTERNAL_SERVER_ERROR.reasonPhrase), INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElement(ex: NoSuchElementException, request: WebRequest): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(NOT_FOUND.reasonPhrase), NOT_FOUND)
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(ex: EntityNotFoundException, request: WebRequest): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(NOT_FOUND.reasonPhrase), NOT_FOUND)
    }

}
