package rs.russian.portal.security.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import rs.russian.portal.security.auth.AuthenticationFailed
import rs.russian.portal.user.UserService

/**
 * JwtAuthenticationFilter is a class that handles JWT authentication for incoming requests.
 * It extracts the JWT token from the request header, validates the token,
 * and sets the authenticated user in the SecurityContextHolder if the token is valid.
 *
 * @property jwtService the service used to handle JWT operations
 * @property userService the service used to load user details from the database
 */
@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userService: UserService
): OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        request.extractToken()?.let { jwt ->
            val username = jwtService.extractUsername(jwt)
            if (username.isEmpty() || !jwtService.isTokenValid(jwt)) {
                throw AuthenticationFailed()
            }
            if (SecurityContextHolder.getContext().authentication == null) {
                val userDetails = userService.loadUserByUsername(username)
                val authToken = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
                val context = SecurityContextHolder.createEmptyContext().also { it.authentication = authToken }
                SecurityContextHolder.setContext(context)
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun HttpServletRequest.extractToken(): String? {
        val authHeader = this.getHeader(AUTHORIZATION)
        return if (authHeader.containsToken()) authHeader.extractToken() else null
    }

    private fun String?.containsToken() = this != null && this.startsWith(BEARER_TYPE)

    private fun String.extractToken() = this.substringAfter(BEARER_TYPE)

    companion object {
        private const val BEARER_TYPE = "Bearer "
    }
}
