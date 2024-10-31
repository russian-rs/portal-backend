package rs.russian.portal.user

import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.UserInfo
import rs.russian.portal.user.domain.UserRepository

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper
) {

    @Transactional(readOnly = true)
    fun getAccount(id: String): Account = userRepository.findById(id).orElseThrow()

    fun getAccountByLogin(login: String): Account = userRepository.findByUsername(login).orElseThrow()

    @Transactional
    fun createOrUpdateAccount(oidcUser: OidcUser) {
        userRepository.findById(oidcUser.userInfo.subject).ifPresentOrElse({
            userMapper.update(oidcUser.userInfo, it)
            if (it.info === null) {
                it.info = UserInfo(account = it)
            }
        }, {
            val account = userMapper.map(oidcUser.userInfo)
            userRepository.saveAndFlush(account)
            account.info = UserInfo(account = account)
        })
    }
}
