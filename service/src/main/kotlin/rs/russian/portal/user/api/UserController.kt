package rs.russian.portal.user.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.UserApi
import rs.russian.generated.model.PageRequest
import rs.russian.generated.model.UserInfoDto
import rs.russian.generated.model.UserPageResponse
import rs.russian.portal.shared.jpa.convert
import rs.russian.portal.shared.security.currentUserId
import rs.russian.portal.user.mapper.UserMapper
import rs.russian.portal.user.service.AccountService
import rs.russian.portal.user.service.SessionService

@RestController
class UserController(
    private val accountService: AccountService,
    private val sessionService: SessionService,
    private val userMapper: UserMapper
) : UserApi {

    override fun getCurrentAccount(): ResponseEntity<UserInfoDto> {
        val user = accountService.getAccount(currentUserId())
        return ResponseEntity.ok(userMapper.map(user.info))
    }

    override fun getInfo(login: String): ResponseEntity<UserInfoDto> {
        val user = accountService.getAccountByLogin(login)
        return ResponseEntity.ok(userMapper.map(user.info))
    }

    override fun logout(all: Boolean): ResponseEntity<Unit> {
        sessionService.invalidate(all)
        return ResponseEntity.ok(null)
    }

    override fun resolveUsers(requestBody: List<String>): ResponseEntity<List<UserInfoDto>> {
        val accounts = accountService.resolve(requestBody)
        return ResponseEntity.ok(accounts.map { userMapper.map(it.info) })
    }

    override fun searchUsers(searchQuery: String, pageRequest: PageRequest): ResponseEntity<UserPageResponse> {
        val page = accountService.search(searchQuery, pageRequest)
        return ResponseEntity.ok(
            UserPageResponse(
                page = convert(page),
                content = page.map { userMapper.map(it.info) }.toMutableList()
            )
        )
    }

    override fun setAvatar(avatarId: String): ResponseEntity<UserInfoDto> {
        return ResponseEntity.ok(userMapper.map(accountService.setAvatar(avatarId).info))
    }

}
