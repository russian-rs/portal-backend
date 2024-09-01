package rs.russian.portal.security.auth

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.AuthenticationResponse
import rs.russian.portal.security.domain.UserTokenRepository
import rs.russian.portal.security.jwt.JwtService
import rs.russian.portal.security.utils.currentUser
import rs.russian.portal.user.domain.UserProfile

@Service
class AuthenticationService(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService,
    private val userTokenRepository: UserTokenRepository,
    private val authenticationManager: AuthenticationManager
) {

    /**
     * Authenticates a user with the provided username and password.
     * Returns the access token and refresh token for the authenticated user.
     *
     * @param username the username of the user
     * @param password the password of the user
     * @return the authentication response containing the access token and refresh token
     */
    @Transactional
    fun authenticate(username: String, password: String): AuthenticationResponse {
        authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
        val user = userDetailsService.loadUserByUsername(username) as UserProfile
        val accessToken = jwtService.generateAccessToken(user).also { user.addToken(it) }
        val refreshToken = jwtService.generateRefreshToken(user).also { user.addToken(it) }
        return AuthenticationResponse(accessToken.token, refreshToken.token)
    }

    /**
     * Logs out the currently authenticated user from the system.
     *
     * @param allDevices true if all devices should be logged out, false otherwise
     * @param accessToken the access token of the currently authenticated user
     * @param refreshToken the refresh token of the currently authenticated user
     * @throws AuthenticationFailed if the accessToken does not belong to the current user
     */
    @Transactional
    fun logout(allDevices: Boolean, accessToken: String, refreshToken: String) {
        if (currentUser().username != jwtService.extractUsername(accessToken)) {
            throw AuthenticationFailed()
        }
        if (allDevices) {
            userTokenRepository.deleteAllByUser(currentUser())
        } else {
            userTokenRepository.deleteByToken(accessToken)
            userTokenRepository.deleteByToken(refreshToken)
        }
    }

    /**
     * Refreshes the access token using the provided refresh token.
     *
     * @param refreshToken the refresh token
     * @return the authentication response containing the new access token and the refresh token
     * @throws AuthenticationFailed if the refresh token is invalid
     */
    @Transactional
    fun refreshAccessToken(refreshToken: String): AuthenticationResponse {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw AuthenticationFailed()
        }
        val user = userDetailsService.loadUserByUsername(jwtService.extractUsername(refreshToken)) as UserProfile
        val accessToken = jwtService.generateAccessToken(user).also { user.addToken(it) }
        return AuthenticationResponse(accessToken.token, refreshToken)
    }
}
