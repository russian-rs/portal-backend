package rs.russian.portal.shared.security

import org.slf4j.LoggerFactory
import java.net.URI

/**
 * Validates frontend URI to prevent open redirect vulnerabilities.
 * Only allows localhost (development) and *.russian.rs (production).
 */
object FrontendUriValidator {

    private val log = LoggerFactory.getLogger(FrontendUriValidator::class.java)
    private const val TRUSTED_DOMAIN = "russian.rs"

    /**
     * Validates and normalizes a frontend URI. Throws on invalid URI.
     * Supports patterns like http://localhost:* for CORS configuration.
     */
    fun validate(url: String): String {
        try {
            if (isLocalhost(url)) {
                log.info("Frontend URI configured for development: {}", url)
                return url
            }

            val uri = URI(url)
            val host = uri.host ?: throw IllegalArgumentException("Frontend URI must have a host: $url")

            // Allow trusted domain and subdomains
            if (host == TRUSTED_DOMAIN || host.endsWith(".$TRUSTED_DOMAIN")) {
                if (uri.scheme != "https") {
                    val httpsUri = URI("https", uri.userInfo, uri.host, uri.port, uri.path, uri.query, uri.fragment).toString()
                    log.warn("Frontend URI upgraded to HTTPS: {} -> {}", url, httpsUri)
                    return httpsUri
                }
                log.info("Frontend URI configured: {}", url)
                return url
            }

            throw IllegalArgumentException("Frontend URI must be localhost or *.$TRUSTED_DOMAIN, got: $host")
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid frontend URI '$url': ${e.message}", e)
        }
    }

    /**
     * Validates without throwing. Returns null if invalid or blank.
     */
    fun validateOrNull(url: String): String? =
        url.trim().takeIf { it.isNotBlank() }?.let {
            runCatching { validate(it) }.getOrNull()
        }

    /**
     * Checks if host or URL is localhost (supports patterns like http://localhost:*).
     */
    fun isLocalhost(hostOrUrl: String): Boolean {
        val normalized = hostOrUrl.replace(":*", ":0") // normalize pattern for URI parsing
        val host = if (normalized.contains("://")) {
            runCatching { URI(normalized).host }.getOrNull()
        } else {
            normalized.substringBefore(":")
        }
        return host == "localhost" || host == "127.0.0.1"
    }
}
