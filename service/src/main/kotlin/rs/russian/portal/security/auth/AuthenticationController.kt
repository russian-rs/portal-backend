package rs.russian.portal.security.auth

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.AuthApi
import rs.russian.generated.model.AuthenticationRequest
import rs.russian.generated.model.AuthenticationResponse
import rs.russian.generated.model.LogoutRequest
import rs.russian.generated.model.RefreshTokenRequest

@RestController
class AuthenticationController(
    private val authenticationService: AuthenticationService
) : AuthApi {

    override fun authenticate(authenticationRequest: AuthenticationRequest): ResponseEntity<AuthenticationResponse> {
        val username = authenticationRequest.username
        val password = authenticationRequest.password
        return ResponseEntity.ok(authenticationService.authenticate(username, password))
    }

    override fun logout(logoutRequest: LogoutRequest, allDevices: Boolean): ResponseEntity<Unit> {
        authenticationService.logout(allDevices, logoutRequest.accessToken, logoutRequest.refreshToken)
        return ResponseEntity.ok().build()
    }

    override fun refresh(refreshTokenRequest: RefreshTokenRequest): ResponseEntity<AuthenticationResponse> {
        return ResponseEntity.ok(authenticationService.refreshAccessToken(refreshTokenRequest.refreshToken))
    }

}
