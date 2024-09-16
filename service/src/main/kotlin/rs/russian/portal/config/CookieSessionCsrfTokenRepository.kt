package rs.russian.portal.config

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository

class CookieSessionCsrfTokenRepository : CsrfTokenRepository {

    companion object {
        const val DEFAULT_CSRF_COOKIE_NAME: String = "XSRF-TOKEN"
    }

    private val sessionTokenRepository = HttpSessionCsrfTokenRepository()

    override fun generateToken(request: HttpServletRequest): CsrfToken {
        return sessionTokenRepository.generateToken(request)
    }

    override fun saveToken(csrfToken: CsrfToken?, request: HttpServletRequest, response: HttpServletResponse) {
        sessionTokenRepository.saveToken(csrfToken, request, response)
        val cookie = Cookie(DEFAULT_CSRF_COOKIE_NAME, csrfToken?.token).also {
            it.path = "/"
            it.setAttribute("SameSite", "Lax")
        }
        response.addCookie(cookie)
    }

    override fun loadToken(request: HttpServletRequest): CsrfToken? {
        return sessionTokenRepository.loadToken(request)
    }
}
