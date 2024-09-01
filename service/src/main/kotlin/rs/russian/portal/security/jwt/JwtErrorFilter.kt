package rs.russian.portal.security.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import rs.russian.generated.model.ErrorCode.AUTHENTICATION_FAILED
import rs.russian.generated.model.ErrorResponse

@Component
class JwtErrorFilter(
    private val objectMapper: ObjectMapper
): OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (e: ExpiredJwtException) {
            response.writer.write(objectMapper.writeValueAsString(ErrorResponse(AUTHENTICATION_FAILED, "Token expired")))
            response.status = HttpStatus.UNAUTHORIZED.value()
        } catch (e: JwtException) {
            response.writer.write(objectMapper.writeValueAsString(ErrorResponse(AUTHENTICATION_FAILED, "Invalid token")))
            response.status = HttpStatus.UNAUTHORIZED.value()
        }
    }
}
