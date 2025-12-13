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
        // Just accessing the token forces it to be generated and set in the cookie
        return ResponseEntity.ok().build()
    }
}