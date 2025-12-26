package rs.russian.portal.shared.api

import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoint to retrieve CSRF token for SPA frontend.
 * When accessed, Spring Security will set the XSRF-TOKEN cookie.
 */
@RestController
@RequestMapping("/csrf")
class CsrfController {

    @GetMapping
    fun getCsrfToken(csrfToken: CsrfToken): ResponseEntity<Void> {
        // With Spring Security 6, tokens are loaded lazily (deferred).
        // We must explicitly call getToken() to force the token to be generated
        // and the XSRF-TOKEN cookie to be set.
        csrfToken.token
        return ResponseEntity.ok().build()
    }
}