package rs.russian.portal.security.utils

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class CustomPasswordEncoder: PasswordEncoder {

    private val bCryptPasswordEncoder = BCryptPasswordEncoder()
    private val phPasswordEncoder = PHPasswordEncoder() // for migrated WordPress users

    /**
     * All new passwords should be encoded with BCrypt
     */
    override fun encode(rawPassword: CharSequence): String = bCryptPasswordEncoder.encode(rawPassword)

    override fun matches(rawPassword: CharSequence, encodedPassword: String): Boolean {
        return if (encodedPassword.startsWith(PHPASS_PREFIX)) {
            phPasswordEncoder.matches(rawPassword, encodedPassword)
        } else {
            bCryptPasswordEncoder.matches(rawPassword, encodedPassword)
        }
    }

    companion object {
        private const val PHPASS_PREFIX = "\$P\$B"
    }
}
