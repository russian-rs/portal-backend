package rs.russian.portal.user.service

import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.shared.security.currentUserId
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.UserInfo
import rs.russian.portal.user.mapper.UserMapper
import rs.russian.portal.user.repository.AccountRepository

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val userMapper: UserMapper
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
                it.info = UserInfo(account = it)
            }
        }, {
            val account = userMapper.map(oidcUser.userInfo)
            accountRepository.saveAndFlush(account)
            account.info = UserInfo(account = account)
        })
    }
}
