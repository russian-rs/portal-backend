package rs.russian.portal.user.service.wordpress

import org.wordpress.model.WpUser

interface WordpressUserService {

    fun getUser(username: String): WpUser?

    fun createUser(user: WpUser): WpUser

    fun updateUser(user: WpUser): WpUser

    fun deleteUser(username: String)

}
