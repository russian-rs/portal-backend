package rs.russian.portal.shared.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper

/**
 * Фильтр для логирования запросов от сервисных аккаунтов.
 * Логирует метод, путь и тело запроса.
 */
class ServiceAccountLoggingFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val wrappedRequest =
            request as? ContentCachingRequestWrapper ?: ContentCachingRequestWrapper(request)

        try {
            filterChain.doFilter(wrappedRequest, response)
        } finally {
            val authentication = currentAuthentication()
            val isServiceAccount = if (authentication is JwtAuthenticationToken) {
                authentication.tokenAttributes["is_service_account"] as? Boolean ?: false
            } else {
                false
            }

            if (isServiceAccount) {
                logServiceRequest(wrappedRequest, authentication as JwtAuthenticationToken)
            }
        }
    }

    private fun logServiceRequest(request: ContentCachingRequestWrapper, auth: JwtAuthenticationToken) {
        val login = currentUserLogin() ?: "unknown"
        val method = request.method
        val path = request.requestURI
        val queryString = request.queryString?.let { "?$it" } ?: ""
        val payload = String(request.contentAsByteArray, charset(request.characterEncoding))
            .replace("\n", " ")
            .replace("\r", " ")
            .trim()

        log.info("Service account request [{}]: {} {}{} | Body: {}", login, method, path, queryString, payload)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ServiceAccountLoggingFilter::class.java)
    }
}
