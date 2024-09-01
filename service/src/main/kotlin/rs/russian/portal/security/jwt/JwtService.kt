package rs.russian.portal.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.security.auth.AuthenticationProperties
import rs.russian.portal.security.domain.TokenType
import rs.russian.portal.security.domain.UserToken
import rs.russian.portal.security.domain.UserTokenRepository
import rs.russian.portal.user.domain.UserProfile
import java.util.*
import kotlin.reflect.KFunction1

@Service
class JwtService(
    private val repository: UserTokenRepository,
    private val securityProperties: AuthenticationProperties
) {

    fun extractUsername(token: String): String = extractClaim(token, Claims::getSubject)

    @Transactional(readOnly = true)
    fun isTokenValid(token: String): Boolean =
        extractClaim(token, Claims::getExpiration).after(Date()) && repository.existsByToken(token)

    /**
     * Generates a JWT access token for the given user details.
     *
     * @param userDetails the user details object containing the username
     * @return the generated JWT access token
     */
    fun generateAccessToken(userDetails: UserDetails): UserToken {
        val expiration = securityProperties.getAccessTokenExpiration()
        val jwt = Jwts.builder()
            .subject(userDetails.username)
            .issuedAt(Date())
            .expiration(expiration)
            .claim(UID_CLAIM, UUID.randomUUID())
            .signWith(securityProperties.getJwtSigningKey())
            .compact()
        return UserToken(token = jwt, validUntil = expiration, type = TokenType.ACCESS, user = userDetails as UserProfile)
    }

    /**
     * Generates a JWT refresh token for the given user details.
     *
     * @param userDetails the user details object containing the username
     * @return the generated JWT refresh token
     */
    fun generateRefreshToken(userDetails: UserDetails): UserToken {
        val expiration = securityProperties.getRefreshTokenExpiration()
        val jwt = Jwts.builder()
            .subject(userDetails.username)
            .issuedAt(Date())
            .expiration(expiration)
            .claim(UID_CLAIM, UUID.randomUUID())
            .signWith(securityProperties.getJwtSigningKey())
            .compact()
        return UserToken(token = jwt, validUntil = expiration, type = TokenType.REFRESH, user = userDetails as UserProfile)
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

    companion object {
        private const val UID_CLAIM = "uid"
    }
}
