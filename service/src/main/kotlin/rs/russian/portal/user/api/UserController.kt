package rs.russian.portal.user.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.UserApi
import rs.russian.generated.model.*
import rs.russian.portal.shared.exception.NotAuthorizedException
import rs.russian.portal.shared.jpa.convert
import rs.russian.portal.shared.security.Authorized
import rs.russian.portal.user.domain.enums.UserGroup.ADMIN_SSO
import rs.russian.portal.user.domain.enums.UserGroup.ADMIN_VOLUNTEER
import rs.russian.portal.user.mapper.UserMapper
import rs.russian.portal.user.service.AccountService
import rs.russian.portal.user.service.SessionService

@RestController
class UserController(
    private val accountService: AccountService,
    private val sessionService: SessionService,
    private val userMapper: UserMapper,
) : UserApi {

    override fun getCurrentAccount(): ResponseEntity<UserInfoDto> {
        val account = accountService.getCurrentAccount()
        return ResponseEntity.ok(userMapper.map(account.info))
    }

    override fun getInfo(login: String): ResponseEntity<UserInfoDto> {
        val account = accountService.getAccountByLogin(login)
        return ResponseEntity.ok(userMapper.map(account.info))
    }

    override fun logout(all: Boolean): ResponseEntity<Unit> {
        sessionService.invalidate(all)
        return ResponseEntity.ok(null)
    }

    override fun resolveUsers(requestBody: List<String>): ResponseEntity<List<UserInfoDto>> {
        val accounts = accountService.resolve(requestBody)
        return ResponseEntity.ok(accounts.map { userMapper.map(it.info) })
    }

    override fun searchUsers(
        searchQuery: String,
        pageRequest: PageRequest,
        userSearchFilter: UserSearchFilter?,
    ): ResponseEntity<UserPageResponse> {
        val page = accountService.search(searchQuery, pageRequest, userSearchFilter)
        return ResponseEntity.ok(
            UserPageResponse(
                page = convert(page),
                content = page.map { userMapper.map(it.info) }.toMutableList()
            )
        )
    }

    override fun setAvatar(avatarId: String): ResponseEntity<UserInfoDto> {
        val currentUser = accountService.getCurrentAccount()
        return ResponseEntity.ok(userMapper.map(accountService.setAvatar(currentUser.id!!, avatarId).info))
    }

    override fun setProgram(id: Int, code: String): ResponseEntity<UserInfoDto> {
        return ResponseEntity.ok(userMapper.map(accountService.setProgram(id, code).info))
    }

    override fun setProject(id: Int, code: String): ResponseEntity<UserInfoDto> {
        return ResponseEntity.ok(userMapper.map(accountService.setProject(id, code).info))
    }

    @Authorized(allowed = [ADMIN_SSO, ADMIN_VOLUNTEER])
    override fun createUser(userCreateRequest: UserCreateRequest): ResponseEntity<UserInfoDto> {
        val account = accountService.create(userCreateRequest)
        return ResponseEntity.ok(userMapper.map(account.info))
    }

    @Authorized(allowed = [ADMIN_SSO, ADMIN_VOLUNTEER])
    override fun activateAccount(id: Int): ResponseEntity<UserInfoDto> {
        return ResponseEntity.ok(userMapper.map(accountService.switchActiveState(id, true).info))
    }

    @Authorized(allowed = [ADMIN_SSO, ADMIN_VOLUNTEER])
    override fun deactivateAccount(id: Int): ResponseEntity<UserInfoDto> {
        return ResponseEntity.ok(userMapper.map(accountService.switchActiveState(id, false).info))
    }

    @Authorized(allowed = [ADMIN_VOLUNTEER])
    override fun updateContracts(id: Int, contractDto: List<ContractDto>): ResponseEntity<UserInfoDto> {
        return ResponseEntity.ok(userMapper.map(accountService.updateContracts(id, HashSet(contractDto)).info))
    }

    override fun updateInfo(login: String, userInfoUpdateRequest: UserInfoUpdateRequest): ResponseEntity<UserInfoDto> {
        val targetAccount = accountService.getAccountByLogin(login)
        val currentAccount = accountService.getCurrentAccount()

        if (!(currentAccount.groups.contains(ADMIN_SSO) || currentAccount.groups.contains(ADMIN_VOLUNTEER))) {
            if (currentAccount.username != login) {
                throw NotAuthorizedException()
            }
        }

        return ResponseEntity.ok(
            userMapper.map(
                accountService.partialUpdateInfo(
                    targetAccount.id!!,
                    userInfoUpdateRequest
                ).info
            )
        )
    }
}
