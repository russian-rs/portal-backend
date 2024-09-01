package rs.russian.portal.security.auth

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.filter.OncePerRequestFilter
import rs.russian.generated.model.ErrorCode.*
import rs.russian.generated.model.ErrorResponse

@ControllerAdvice
class AuthenticationErrorHandler(private val objectMapper: ObjectMapper): OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (e: UsernameNotFoundException) {
            response.writer.write(objectMapper.writeValueAsString(ErrorResponse(BAD_CREDENTIALS, "User not found")))
            response.status = HttpStatus.UNAUTHORIZED.value()
        } catch (e: AuthenticationFailed) {
            response.writer.write(objectMapper.writeValueAsString(ErrorResponse(AUTHENTICATION_FAILED, "Invalid authentication")))
            response.status = HttpStatus.UNAUTHORIZED.value()
        } catch (e: AuthenticationException) {
            response.writer.write(objectMapper.writeValueAsString(ErrorResponse(AUTHENTICATION_FAILED, "Invalid authentication")))
            response.status = HttpStatus.UNAUTHORIZED.value()
        } finally {
            response.addHeader("Content-Type", "application/json")
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(response: HttpServletResponse) {
        response.addHeader("Content-Type", "application/json")
        response.writer.write(objectMapper.writeValueAsString(
            ErrorResponse(AUTHENTICATION_FAILED, "Invalid authentication")))
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(CredentialsExpiredException::class)
    fun handleCredentialsExpiredException(response: HttpServletResponse) {
        response.addHeader("Content-Type", "application/json")
        response.writer.write(objectMapper.writeValueAsString(
            ErrorResponse(CREDENTIALS_EXPIRED, "Credentials expired")))
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(response: HttpServletResponse) {
        response.addHeader("Content-Type", "application/json")
        response.writer.write(objectMapper.writeValueAsString(
            ErrorResponse(BAD_CREDENTIALS, "Bad credentials")))
    }
}
