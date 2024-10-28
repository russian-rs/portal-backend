package rs.russian.portal.user

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.UserApi
import rs.russian.generated.model.UserInfoDto
import rs.russian.generated.model.UserProfileDto
import rs.russian.portal.shared.security.currentUserId

@RestController
class UserController(
    private val userService: UserService,
    private val sessionService: SessionService,
    private val userProfileMapper: UserProfileMapper
) : UserApi {

    override fun getProfile(): ResponseEntity<UserProfileDto> {
        val user = userService.getUser(currentUserId())
        return ResponseEntity.ok(userProfileMapper.map(user))
    }

    override fun getInfo(): ResponseEntity<UserInfoDto> {
        TODO("Not yet implemented")
    }

    override fun logout(all: Boolean): ResponseEntity<Unit> {
        sessionService.invalidate(all)
        return ResponseEntity.ok(null)
    }

}
