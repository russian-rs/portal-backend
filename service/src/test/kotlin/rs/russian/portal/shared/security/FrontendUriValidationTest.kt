package rs.russian.portal.shared.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Tests for FrontendUriValidator.
 * Validates that open redirect vulnerabilities are prevented at application startup.
 */
class FrontendUriValidationTest {

    @ParameterizedTest
    @ValueSource(strings = [
        "http://localhost:3000",
        "http://localhost:*",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:*",
        "https://russian.rs",
        "https://portal.russian.rs",
        "https://REDACTED_HOST",
        "https://api.portal.russian.rs",
    ])
    fun `should accept valid URLs`(url: String) {
        assertEquals(url, FrontendUriValidator.validate(url))
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "http://localhost:3000",
        "http://localhost:*",
        "http://localhost:5173",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:*",
        "localhost",
        "127.0.0.1",
    ])
    fun `isLocalhost should return true for localhost`(hostOrUrl: String) {
        assertTrue(FrontendUriValidator.isLocalhost(hostOrUrl))
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "https://russian.rs",
        "https://evil.com",
        "google.com",
    ])
    fun `isLocalhost should return false for non-localhost`(hostOrUrl: String) {
        assertFalse(FrontendUriValidator.isLocalhost(hostOrUrl))
    }

    @Test
    fun `should upgrade HTTP to HTTPS for production`() {
        assertEquals("https://portal.russian.rs", FrontendUriValidator.validate("http://portal.russian.rs"))
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "https://evil.com",
        "https://google.com",
        "https://attacker.com/phishing",
        "https://fakerussian.rs",
        "https://evil-russian.rs",
        "https://russian.rs.evil.com",
        "https://russian.rs.attacker.com",
        "ftp://evil.com/file",
    ])
    fun `should reject untrusted URLs`(url: String) {
        assertThrows<IllegalArgumentException> {
            FrontendUriValidator.validate(url)
        }
    }

    @Test
    fun `should reject malformed URLs`() {
        assertThrows<IllegalArgumentException> {
            FrontendUriValidator.validate("not a valid url")
        }
    }

    @Test
    fun `should reject URLs without host`() {
        val exception = assertThrows<IllegalArgumentException> {
            FrontendUriValidator.validate("/login")
        }
        assertTrue(exception.message!!.contains("must have a host"))
    }
}
