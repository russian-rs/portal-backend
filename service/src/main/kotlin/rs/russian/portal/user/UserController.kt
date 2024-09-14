package rs.russian.portal.user

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.UserApi
import rs.russian.generated.model.UserInfo
import rs.russian.portal.shared.security.currentUserId

@RestController
class UserController(
    private val userService: UserService,
    private val userProfileMapper: UserProfileMapper
) : UserApi {

    override fun info(): ResponseEntity<UserInfo> {
        val user = userService.getUser(currentUserId())
        return ResponseEntity.ok(userProfileMapper.map(user))
    }

}
