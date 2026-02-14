package rs.russian.portal.user.service.wordpress

import org.wordpress.api.CustomWordpressApi
import org.wordpress.api.UsersWordpressApi
import org.wordpress.model.WpRole
import org.wordpress.model.WpUser

interface WordpressUserService {
    val instanceName: String
    var apiClient: UsersWordpressApi
    var customWpApi: CustomWordpressApi

    fun getUser(email: String): WpUser?

    fun createUser(user: WpUser): WpUser

    fun updateUser(user: WpUser): WpUser

    fun deleteUser(email: String)

    fun getAvailableRoles(): List<WpRole>
}
