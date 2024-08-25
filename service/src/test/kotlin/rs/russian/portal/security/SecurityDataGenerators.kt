package rs.russian.portal.security

import io.jsonwebtoken.Jwts.SIG
import io.jsonwebtoken.io.Encoders
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * It is not a test, just an example on how to generate key for JWT singing
 */
class SecurityDataGenerators {

    @Test
    @DisplayName("JWT Singing HMAC key")
    fun jwtSingingKey() {
        val encodedKey = SIG.HS512.key().build().encoded
        val decodedKey = Keys.hmacShaKeyFor(encodedKey)
        println("BASE64:")
        println(Encoders.BASE64.encode(decodedKey.encoded))
    }

    @Test
    @DisplayName("BCrypt password hash")
    fun bcryptPasswordHash() {
        val bCryptPasswordEncoder = BCryptPasswordEncoder()
        println(bCryptPasswordEncoder.encode("12345678"))
    }

}
