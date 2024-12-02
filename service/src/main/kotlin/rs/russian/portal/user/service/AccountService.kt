package rs.russian.portal.user.service

import io.authentik.model.User
import org.springframework.data.domain.Page
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.PageRequest
import rs.russian.portal.file.service.FileService
import rs.russian.portal.shared.jpa.convert
import rs.russian.portal.shared.security.currentUserLogin
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.UserInfo
import rs.russian.portal.user.domain.specification.searchSpecification
import rs.russian.portal.user.mapper.UserMapper
import rs.russian.portal.user.repository.AccountRepository

@Service
class AccountService(
    private val userMapper: UserMapper,
    private val fileService: FileService,
    private val accountRepository: AccountRepository,
    private val authentikUserService: AuthentikUserService
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
    fun getCurrentAccount(): Account = getAccountByLogin(currentUserLogin())

    @Transactional
    fun save(account: Account): Account {
        return accountRepository.saveAndFlush(account)
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
    fun search(query: String, pageRequest: PageRequest): Page<Account> {
        val specification = searchSpecification(query)
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
}
