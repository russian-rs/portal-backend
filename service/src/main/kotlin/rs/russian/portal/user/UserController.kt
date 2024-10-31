package rs.russian.portal.user

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.UserApi
import rs.russian.generated.model.AccountDto
import rs.russian.generated.model.UserInfoDto
import rs.russian.portal.shared.security.currentUserId

@RestController
class UserController(
    private val userService: UserService,
    private val sessionService: SessionService,
    private val userMapper: UserMapper
) : UserApi {

    override fun getCurrentAccount(): ResponseEntity<AccountDto> {
        val user = userService.getAccount(currentUserId())
        return ResponseEntity.ok(userMapper.map(user))
    }

    override fun getInfo(login: String): ResponseEntity<UserInfoDto> {
        val user = userService.getAccountByLogin(login)
        return ResponseEntity.ok(userMapper.map(user.info))
    }

    override fun logout(all: Boolean): ResponseEntity<Unit> {
        sessionService.invalidate(all)
        return ResponseEntity.ok(null)
    }

}
