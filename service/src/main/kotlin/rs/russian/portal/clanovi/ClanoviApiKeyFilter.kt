package rs.russian.portal.clanovi

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import rs.russian.portal.config.AppProperties
import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8

/**
 * Permanent office key for Clanovi paths. Does not touch website OAuth/cookie login.
 * If [AppProperties.clanovi] key is blank, the API stays closed (401).
 */
class ClanoviApiKeyFilter(
    private val appProperties: AppProperties,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.servletPath.startsWith(PATH_PREFIX)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val expected = appProperties.clanovi.apiKey
        val provided = request.getHeader(HEADER).orEmpty()
        if (expected.isBlank()) {
            log.warn("Clanovi API closed: CLANOVI_API_KEY is empty")
            response.sendError(SC_UNAUTHORIZED)
            return
        }
        if (!keysEqual(expected, provided)) {
            log.warn("Clanovi API key rejected")
            response.sendError(SC_UNAUTHORIZED)
            return
        }
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            PRINCIPAL,
            null,
            listOf(SimpleGrantedAuthority(ROLE)),
        )
        filterChain.doFilter(request, response)
    }

    companion object {
        const val HEADER = "X-Clanovi-Key"
        const val PATH_PREFIX = "/clanovi"
        private const val PRINCIPAL = "clanovi"
        private const val ROLE = "ROLE_CLANOVI"
        private val log = LoggerFactory.getLogger(ClanoviApiKeyFilter::class.java)

        fun keysEqual(expected: String, provided: String): Boolean {
            val left = expected.toByteArray(UTF_8)
            val right = provided.toByteArray(UTF_8)
            if (left.size != right.size) {
                MessageDigest.isEqual(left, left)
                return false
            }
            return MessageDigest.isEqual(left, right)
        }
    }
}
