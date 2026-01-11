package rs.russian.portal.shared.api

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.SecurityApi

/**
 * Endpoint to retrieve CSRF token for SPA frontend.
 * When accessed, Spring Security will set the XSRF-TOKEN cookie.
 */
@RestController
class CsrfController(
    private val request: HttpServletRequest,
) : SecurityApi {

    override fun getCsrfToken(): ResponseEntity<Unit> {
        // With Spring Security 6, tokens are loaded lazily (deferred).
        // We must explicitly call getToken() to force the token to be generated
        // and the XSRF-TOKEN cookie to be set.
        val csrfToken = request.getAttribute("_csrf") as? CsrfToken
        csrfToken?.token
        return ResponseEntity.ok().build()
    }
}