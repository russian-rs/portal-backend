package rs.russian.portal.shared.exception

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpStatus.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.MaxUploadSizeExceededException
import rs.russian.generated.model.ErrorResponse

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        log.error(ex.message, ex)
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

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceededException(
        ex: MaxUploadSizeExceededException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(PAYLOAD_TOO_LARGE.reasonPhrase), PAYLOAD_TOO_LARGE)
    }

    @ExceptionHandler(UnsupportedFileFormat::class)
    fun handleUnsupportedFileFormat(ex: UnsupportedFileFormat, request: WebRequest): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(UNSUPPORTED_MEDIA_TYPE.reasonPhrase), UNSUPPORTED_MEDIA_TYPE)
    }

    @ExceptionHandler(TypeMismatchException::class)
    fun handleTypeMismatchException(
        ex: TypeMismatchException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(BAD_REQUEST.reasonPhrase), BAD_REQUEST)
    }

    @ExceptionHandler(NotAuthorizedException::class)
    fun handleUnsupportedFileFormat(ex: NotAuthorizedException, request: WebRequest): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(FORBIDDEN.reasonPhrase), FORBIDDEN)
    }

    @ExceptionHandler(CaptchaInvalidException::class)
    fun handleCaptchaInvalidException(ex: CaptchaInvalidException, request: WebRequest): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(FORBIDDEN.reasonPhrase), FORBIDDEN)
    }

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
