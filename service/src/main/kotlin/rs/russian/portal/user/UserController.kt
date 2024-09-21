package rs.russian.portal.user

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.UserApi
import rs.russian.generated.model.UserInfo
import rs.russian.portal.shared.security.currentUserId

@RestController
class UserController(
    private val userService: UserService,
    private val sessionService: SessionService,
    private val userProfileMapper: UserProfileMapper
) : UserApi {

    override fun info(): ResponseEntity<UserInfo> {
        val user = userService.getUser(currentUserId())
        return ResponseEntity.ok(userProfileMapper.map(user))
    }

    override fun logout(all: Boolean): ResponseEntity<Unit> {
        sessionService.invalidate(all)
        return ResponseEntity(HttpHeaders().also {
            it.add(
                "Location",
                "https://id.russian.rs/application/o/portal/end-session/"
            )
        }, HttpStatus.FOUND)
    }

}
