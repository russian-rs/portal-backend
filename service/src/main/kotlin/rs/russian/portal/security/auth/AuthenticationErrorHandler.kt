package rs.russian.portal.security.auth

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.filter.OncePerRequestFilter
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
            response.writer.write(objectMapper.writeValueAsString(ErrorResponse("User not found")))
            response.status = HttpStatus.UNAUTHORIZED.value()
        } catch (e: AuthenticationFailed) {
            response.writer.write(objectMapper.writeValueAsString(ErrorResponse("Invalid authentication")))
            response.status = HttpStatus.UNAUTHORIZED.value()
        } catch (e: AuthenticationException) {
            response.writer.write(objectMapper.writeValueAsString(ErrorResponse("Invalid authentication")))
            response.status = HttpStatus.UNAUTHORIZED.value()
        } finally {
            response.addHeader("Content-Type", "application/json")
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException::class)
    fun handleConflict(response: HttpServletResponse) {
        response.addHeader("Content-Type", "application/json")
        response.writer.write(objectMapper.writeValueAsString(ErrorResponse("Invalid authentication")))
    }
}
