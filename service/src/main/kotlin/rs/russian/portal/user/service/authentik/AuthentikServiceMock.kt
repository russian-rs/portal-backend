package rs.russian.portal.user.service.authentik

import io.authentik.model.User
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import rs.russian.portal.user.domain.Account
import java.util.*
import kotlin.random.Random

@Service
@Profile("no-auth")
class AuthentikServiceMock : AuthentikService {

    override fun getUser(email: String): User {
        return getDefaultUser(email.split("@").first(), email)
    }

    override fun getAllUsers(): List<User> {
        return emptyList()
    }

    override fun createUser(username: String, name: String, email: String): User {
        return getDefaultUser(username, email)
    }

    override fun switchActiveState(account: Account, isActive: Boolean) {}

    private fun getDefaultUser(username: String, email: String): User {
        val uid = UUID.randomUUID()
        return User(
            pk = Random.nextInt(),
            username = username,
            email = email,
            avatar = "",
            name = "John Smith",
            uuid = uid,
            uid = uid.toString(),
            groupsObj = mutableListOf(),
            isSuperuser = false
        )
    }
}
