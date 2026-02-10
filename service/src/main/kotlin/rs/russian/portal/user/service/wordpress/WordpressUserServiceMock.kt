package rs.russian.portal.user.service.wordpress

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.wordpress.api.CustomWordpressApi
import org.wordpress.api.UsersWordpressApi
import org.wordpress.model.WpRole
import org.wordpress.model.WpUser
import rs.russian.portal.user.domain.enums.UserGroup
import kotlin.random.Random

@Service
@Profile("local")
class WordpressUserServiceMock : WordpressUserService {

    override val instanceName: String = "Local"
    override lateinit var apiClient: UsersWordpressApi // will not be inited
    override lateinit var customWpApi: CustomWordpressApi // will not be inited

    override fun getUser(email: String): WpUser? {
        log.info("Mock get user")
        return WpUser(
            id = Random.nextInt(),
            username = email.substringBefore('@'),
            email = email
        )
    }

    override fun createUser(user: WpUser): WpUser {
        log.info("Mock create user {}", user.username)
        return user
    }

    override fun updateUser(user: WpUser): WpUser {
        log.info("Mock update user {}", user.username)
        return user
    }

    override fun deleteUser(email: String) {
        log.info("Mock delete user {}", email)
    }

    override fun getAvailableRoles(): List<WpRole> =
        UserGroup.entries.map { WpRole(it.name, it.name) }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
