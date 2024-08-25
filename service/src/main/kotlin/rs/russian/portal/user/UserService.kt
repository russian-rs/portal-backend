package rs.russian.portal.user

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.user.domain.UserRepository

@Service
class UserService(
    private val userRepository: UserRepository
): UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        return userRepository.findByUsername(username) ?: throw UsernameNotFoundException(username)
    }

}
