package rs.russian.portal.security.auth

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.AuthenticationResponse
import rs.russian.portal.security.jwt.JwtService
import rs.russian.portal.user.domain.User
import rs.russian.portal.user.domain.UserTokenRepository

@Service
class AuthenticationService(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService,
    private val userTokenRepository: UserTokenRepository,
    private val authenticationManager: AuthenticationManager
) {

    @Transactional
    fun authenticate(username: String, password: String): AuthenticationResponse {
        authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
        val user = userDetailsService.loadUserByUsername(username) as User
        val accessToken = jwtService.generateAccessToken(user)
        val refreshToken = jwtService.generateRefreshToken(user).also { user.addToken(it) }
        return AuthenticationResponse(accessToken, refreshToken)
    }

    @Transactional
    fun refreshAccessToken(refreshToken: String): AuthenticationResponse {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw AuthenticationFailed()
        }
        val token = userTokenRepository.findByToken(refreshToken) ?: throw AuthenticationFailed()
        val accessToken = jwtService.generateAccessToken(token.user)
        return AuthenticationResponse(accessToken, refreshToken)
    }
}
