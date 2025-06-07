package rs.russian.portal.user.service.wordpress

import org.wordpress.api.CustomWordpressApi
import org.wordpress.api.UsersWordpressApi
import org.wordpress.model.WpUser
import rs.russian.portal.config.WordpressInstance

class WordpressUserServiceImpl(
    override val instanceName: String,
    override var apiClient: UsersWordpressApi,
    override var customWpApi: CustomWordpressApi,
) : WordpressUserService {

    override fun getAvailableRoles() = customWpApi.rolesList()

    override fun getUser(username: String): WpUser? {
        val users = apiClient.searchUsers(search = username.replace("@", ""))
        return users.find { it.username == username }
    }

    override fun createUser(user: WpUser): WpUser {
        return apiClient.createUser(user)
    }

    override fun updateUser(user: WpUser): WpUser {
        return apiClient.updateUser(user.id, user)
    }

    override fun deleteUser(username: String) {
        getUser(username)?.let {
            apiClient.deleteUser(it.id)
        }
    }
}
