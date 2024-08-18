package rs.russian.portal.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class PHPassCheckerTest {

    private val password = "mybdaj-3Cisbe-puzvum"
    private val hashed = "\$P\$BA.EMog2m/nF8JcAVLquTKs2F2nHRq."

    @Test
    @DisplayName("Хэш пароля из WordPress проходит проверку")
    fun checkPassword() {
        assertTrue(PHPassChecker.check(password, hashed))
    }

    @Test
    @DisplayName("Пустой пароль не проходит проверку")
    fun checkPassword_PasswordIsEmpty() {
        assertFalse(PHPassChecker.check("", hashed))
    }

    @Test
    @DisplayName("Пустой пароль не проходит проверку")
    fun checkPassword_HashIsEmpty() {
        assertFalse(PHPassChecker.check(password, ""))
    }
}
