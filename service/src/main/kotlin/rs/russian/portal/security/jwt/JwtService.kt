package rs.russian.portal.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import rs.russian.portal.security.auth.AuthenticationProperties
import java.util.*
import kotlin.reflect.KFunction1

@Service
class JwtService(
    private val securityProperties: AuthenticationProperties
) {

    fun extractUsername(token: String): String = extractClaim(token, Claims::getSubject)

    fun isTokenValid(token: String): Boolean = extractClaim(token, Claims::getExpiration).after(Date())

    /**
     * Generates a JWT access token for the given user details.
     *
     * @param userDetails the user details object containing the username
     * @return the generated JWT access token
     */
    fun generateAccessToken(userDetails: UserDetails): String {
        return Jwts.builder()
            .subject(userDetails.username)
            .issuedAt(Date())
            .expiration(securityProperties.getAccessTokenExpiration())
            .signWith(securityProperties.getJwtSigningKey())
            .compact()
    }

    /**
     * Generates a JWT refresh token for the given user details.
     *
     * @param userDetails the user details object containing the username
     * @return the generated JWT refresh token
     */
    fun generateRefreshToken(userDetails: UserDetails): String {
        return Jwts.builder()
            .subject(userDetails.username)
            .issuedAt(Date())
            .expiration(securityProperties.getRefreshTokenExpiration())
            .signWith(securityProperties.getJwtSigningKey())
            .compact()
    }

    /**
     * Extracts a claim from a JWT token.
     *
     * @param token the JWT token
     * @param claimsResolvers the function that resolves the desired claim from the token's payload
     * @return the resolved claim
     */
    private fun <T: Any> extractClaim(token: String, claimsResolvers: KFunction1<Claims, T>): T {
        val claims = Jwts.parser()
            .verifyWith(securityProperties.getJwtSigningKey()).build()
            .parseSignedClaims(token)
            .payload
        return claimsResolvers.invoke(claims)
    }
}
