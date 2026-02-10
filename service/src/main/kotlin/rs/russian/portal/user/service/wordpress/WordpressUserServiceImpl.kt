package rs.russian.portal.user.service.wordpress

import org.wordpress.api.CustomWordpressApi
import org.wordpress.api.UsersWordpressApi
import org.wordpress.model.WpUser

class WordpressUserServiceImpl(
    override val instanceName: String,
    override var apiClient: UsersWordpressApi,
    override var customWpApi: CustomWordpressApi,
) : WordpressUserService {

    override fun getAvailableRoles() = customWpApi.rolesList()

    override fun getUser(email: String): WpUser? {
        val users = apiClient.searchUsers(search = email)
        return users.find { it.email == email }
    }

    override fun createUser(user: WpUser): WpUser {
        return apiClient.createUser(user)
    }

    override fun updateUser(user: WpUser): WpUser {
        return apiClient.updateUser(user.id, user)
    }

    override fun deleteUser(email: String) {
        getUser(email)?.let {
            apiClient.deleteUser(it.id)
        }
    }
}
