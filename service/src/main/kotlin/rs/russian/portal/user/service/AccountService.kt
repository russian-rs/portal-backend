package rs.russian.portal.user.service

import org.springframework.data.domain.Page
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.PageRequest
import rs.russian.portal.file.service.FileService
import rs.russian.portal.shared.jpa.convert
import rs.russian.portal.shared.security.currentUserId
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
) {

    @Transactional(readOnly = true)
    fun getAccount(id: String): Account = accountRepository.findById(id).orElseThrow()

    @Transactional(readOnly = true)
    fun getAccountByLogin(login: String): Account = accountRepository.findByUsername(login).orElseThrow()

    @Transactional(readOnly = true)
    fun getCurrentUser(): Account = accountRepository.findById(currentUserId()).orElseThrow()

    @Transactional
    fun createOrUpdateAccount(oidcUser: OidcUser) {
        accountRepository.findById(oidcUser.userInfo.subject).ifPresentOrElse({
            userMapper.update(oidcUser.userInfo, it)
            if (it.info === null) {
                it.info = UserInfo.default(it)
            }
        }, {
            val account = userMapper.map(oidcUser.userInfo)
            accountRepository.saveAndFlush(account)
            account.info = UserInfo.default(account)
        })
    }

    @Transactional(readOnly = true)
    fun search(query: String, pageRequest: PageRequest): Page<Account> {
        val specification = searchSpecification(query)
        return accountRepository.findAll(specification, convert(pageRequest))
    }

    @Transactional
    fun setAvatar(fileId: String): Account {
        val user = accountRepository.findById(currentUserId()).orElseThrow()
        val file = fileService.getFile(fileId)
        user.info?.avatar = file
        return user
    }
}
