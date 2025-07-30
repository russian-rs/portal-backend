package rs.russian.portal.user.service

import io.authentik.model.User
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.*
import rs.russian.portal.file.service.FileService
import rs.russian.portal.shared.exception.NotAuthorizedException
import rs.russian.portal.shared.jpa.convert
import rs.russian.portal.shared.security.currentUserLogin
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.UserInfo
import rs.russian.portal.user.domain.specification.searchSpecification
import rs.russian.portal.user.mapper.ContractMapper
import rs.russian.portal.user.mapper.UserMapper
import rs.russian.portal.user.repository.AccountRepository
import rs.russian.portal.program.repository.ProgramRepository
import rs.russian.portal.program.repository.ProjectRepository
import rs.russian.portal.user.domain.enums.Gender
import rs.russian.portal.user.repository.projections.AgeSliceCountProjection
import rs.russian.portal.user.repository.projections.UsersStatisticGroupCountProjection
import rs.russian.portal.user.service.authentik.AuthentikService
import rs.russian.portal.user.service.wordpress.MultiWordpressUserService

@Service
class AccountService(
    private val userMapper: UserMapper,
    private val programRepository: ProgramRepository,
    private val projectRepository: ProjectRepository,
    private val fileService: FileService,
    private val contractMapper: ContractMapper,
    private val accountRepository: AccountRepository,
    private val multiWordpressUserService: MultiWordpressUserService,
    private val authentikUserService: AuthentikService
) {

    @Transactional(readOnly = true)
    fun getAccount(id: Int): Account = accountRepository.findById(id).orElseThrow()

    @Transactional(readOnly = true)
    fun getAccountByLogin(login: String): Account = accountRepository.findByUsername(login).orElseThrow()

    @Transactional(readOnly = true)
    fun findAccountByLogin(login: String?): Account? {
        if (login.isNullOrBlank()) return null
        return accountRepository.findByUsername(login).orElse(null)
    }

    @Transactional(readOnly = true)
    fun findAccountByEmail(email: String?): Account? {
        if (email.isNullOrBlank()) return null
        return accountRepository.findByEmail(email).orElse(null)
    }

    @Transactional(readOnly = true)
    fun getCurrentAccount(): Account = getAccountByLogin(currentUserLogin() ?: throw NotAuthorizedException())

    @Transactional
    fun save(account: Account): Account {
        return accountRepository.saveAndFlush(account)
    }

    @Transactional
    fun create(request: UserCreateRequest): Account {
        val ssoUser = authentikUserService.createUser(request.username, request.fullName, request.email)
        var account = userMapper.map(ssoUser)
        account.info = UserInfo.default(account)
        account.contracts = mutableListOf(userMapper.map(request.contract, account))
        account = accountRepository.saveAndFlush(account)
        multiWordpressUserService.syncToAll(listOf(account))
        return account
    }

    @Transactional
    fun create(email: String, fullName: String): Account {
        val username = email.split("@")[0].lowercase()
        val ssoUser = authentikUserService.createUser(username, fullName, email)
        var account = userMapper.map(ssoUser)
        account.info = UserInfo.default(account)
        account = accountRepository.saveAndFlush(account)
        multiWordpressUserService.syncToAll(listOf(account))
        return account
    }

    @Transactional
    fun createOrUpdateAccount(oidcUser: OidcUser) {
        val email = oidcUser.userInfo.email
        val id = authentikUserService.getUser(email)!!.pk
        accountRepository.findById(id).ifPresentOrElse({
            userMapper.update(oidcUser.userInfo, it)
            it.info = it.info ?: UserInfo.default(it)
            accountRepository.saveAndFlush(it)
        }, {
            val account = userMapper.map(oidcUser.userInfo)
            account.id = id
            account.info = UserInfo.default(account)
            accountRepository.saveAndFlush(account)
        })
    }

    @Transactional
    fun createOrUpdateAccount(ssoUser: User): Account {
        accountRepository.findById(ssoUser.pk).ifPresentOrElse({
            userMapper.update(ssoUser, it)
            it.info = it.info ?: UserInfo.default(it)
            accountRepository.saveAndFlush(it)
        }, {
            val account = userMapper.map(ssoUser)
            account.info = UserInfo.default(account)
            accountRepository.saveAndFlush(account)
        })
        return getAccount(ssoUser.pk)
    }

    @Transactional(readOnly = true)
    fun search(query: String, pageRequest: PageRequest, filter: UserSearchFilter?): Page<Account> {
        val specification = searchSpecification(query, filter)
        return accountRepository.findAll(specification, convert(pageRequest))
    }

    @Transactional(readOnly = true)
    fun resolve(usernames: List<String>): List<Account> {
        return accountRepository.findAllByUsernameIn(usernames)
    }

    @Transactional
    fun setAvatar(account: Account, fileId: String): Account {
        val file = fileService.getFile(fileId)
        account.info?.avatar = file
        return accountRepository.saveAndFlush(account)
    }

    @Transactional
    fun switchActiveState(account: Account, isActive: Boolean): Account {
        if (account.active == isActive) {
            return account
        }
        account.active = isActive
        authentikUserService.switchActiveState(account, isActive)
        multiWordpressUserService.syncToAll(listOf(account))
        return save(account)
    }

    @Transactional
    fun updateContracts(account: Account, contractList: List<ContractDto>): Account {
        account.contracts.clear()
        account.contracts.addAll(contractList.map { contractMapper.map(it, account) })
        return save(account)
    }

    @Transactional
    fun updateInfo(accountId: Int, newInfo: UserInfo) {
        val account = getAccount(accountId)
        newInfo.id = account.username
        newInfo.account = account
        newInfo.version = account.info?.version
        account.info = newInfo
        accountRepository.save(account)
    }

    @Transactional
    fun partialUpdateInfo(account: Account, userInfoUpdateRequest: UserInfoUpdateRequest): Account {
        val userInfo = account.info ?: UserInfo.default(account)

        userInfoUpdateRequest.city?.let { userInfo.city = it }
        userInfoUpdateRequest.address?.let { userInfo.address = it }
        userInfoUpdateRequest.birthDate?.let { userInfo.birthDate = it }
        userInfoUpdateRequest.telegram?.let { userInfo.telegram = it }
        userInfoUpdateRequest.phone?.let { userInfo.phone = it }
        userInfoUpdateRequest.gender?.let {
            userInfo.gender = Gender.valueOf(it.toString())
        }
        account.info = userInfo

        return save(account)
    }

    @Transactional
    fun setProgram(account: Account, code: String): Account {
        val userInfo = account.info ?: UserInfo.default(account)

        val program = programRepository.findByCode(code)
            ?: throw IllegalArgumentException("Program with code: $code not found!")

        userInfo.program = program
        account.info = userInfo

        return save(account)
    }

    @Transactional
    fun setProject(account: Account, code: String): Account {
        val userInfo = account.info ?: UserInfo.default(account)

        val project = projectRepository.findByCode(code)
            ?: throw IllegalArgumentException("Project with code: $code not found!")

        userInfo.project = project
        account.info = userInfo

        return save(account)
    }

    fun getGenderStatistics(): Map<Gender?, Int> {
        return accountRepository.countByGender().associate { it.gender to it.count }
    }

    fun getAgeSliceStatistics(): AgeSliceCountProjection {
        return accountRepository.countByAgeSlices()
    }

    fun getTotalUserCount(): Int {
        return accountRepository.count().toInt()
    }

    fun getCountByStatisticGroup(): List<UsersStatisticGroupCountProjection> {
        return accountRepository.countByStatisticGroup()
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
