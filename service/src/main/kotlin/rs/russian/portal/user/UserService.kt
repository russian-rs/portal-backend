package rs.russian.portal.user

import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.user.domain.UserProfile
import rs.russian.portal.user.domain.UserRepository

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userProfileMapper: UserProfileMapper
) {

    @Transactional(readOnly = true)
    fun getUser(id: String): UserProfile = userRepository.findById(id).orElseThrow()

    @Transactional
    fun createOrUpdateUser(oidcUser: OidcUser) {
        userRepository.findById(oidcUser.userInfo.subject).ifPresentOrElse({
            userProfileMapper.update(oidcUser.userInfo, it)
        }, {
            val userProfile = userProfileMapper.map(oidcUser.userInfo)
            userRepository.saveAndFlush(userProfile)
        })
    }
}
