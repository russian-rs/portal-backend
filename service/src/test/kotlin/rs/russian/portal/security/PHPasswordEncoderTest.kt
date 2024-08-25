package rs.russian.portal.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rs.russian.portal.security.utils.PHPasswordEncoder
import kotlin.test.assertTrue

class PHPasswordEncoderTest {

    private val password = "mybdaj-3Cisbe-puzvum"
    private val hashed = "\$P\$BA.EMog2m/nF8JcAVLquTKs2F2nHRq."

    private val passwordEncoder = PHPasswordEncoder()

    @Test
    @DisplayName("Хэш пароля из WordPress проходит проверку")
    fun checkPassword() {
        assertTrue(passwordEncoder.matches(password, hashed))
    }

    @Test
    @DisplayName("Пустой пароль не проходит проверку")
    fun checkPassword_PasswordIsEmpty() {
        assertFalse(passwordEncoder.matches("", hashed))
        assertFalse(passwordEncoder.matches(null, hashed))
    }

    @Test
    @DisplayName("Пустой хэш не проходит проверку")
    fun checkPassword_HashIsEmpty() {
        assertFalse(passwordEncoder.matches(password, ""))
        assertFalse(passwordEncoder.matches(password, null))
    }
}
