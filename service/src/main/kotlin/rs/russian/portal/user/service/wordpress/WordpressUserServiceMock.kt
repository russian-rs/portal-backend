package rs.russian.portal.user.service.wordpress

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.wordpress.api.UsersWordpressApi
import org.wordpress.model.WpUser
import kotlin.random.Random

@Service
@Profile("local")
class WordpressUserServiceMock : WordpressUserService {
    override lateinit var apiClient: UsersWordpressApi // will not be inited

    override fun getUser(username: String): WpUser? {
        log.info("Mock get user")
        return WpUser(
            id = Random.nextInt(),
            username = username,
            email = "${username}@russian.rs"
        )
    }

    override fun createUser(user: WpUser): WpUser {
        log.info("Mock create user")
        return user
    }

    override fun updateUser(user: WpUser): WpUser {
        log.info("Mock update user")
        return user
    }

    override fun deleteUser(username: String) {
        log.info("Mock delete user")
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
