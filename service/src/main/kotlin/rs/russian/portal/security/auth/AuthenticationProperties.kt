package rs.russian.portal.security.auth

import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.Date
import javax.crypto.SecretKey

@ConfigurationProperties(prefix = "app.security.auth")
class AuthenticationProperties(
    private val jwtSigningKey: String,
    private val accessTokenExpiration: Long,
    private val refreshTokenExpiration: Long
) {

    fun getAccessTokenExpiration() = Date(Date().time + accessTokenExpiration)

    fun getRefreshTokenExpiration() = Date(Date().time + refreshTokenExpiration)

    fun getJwtSigningKey(): SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSigningKey))
}
