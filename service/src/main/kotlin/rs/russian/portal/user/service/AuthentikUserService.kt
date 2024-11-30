package rs.russian.portal.user.service

import io.authentik.api.CoreAuthentikApi
import io.authentik.model.User
import org.springframework.stereotype.Service

@Service
class AuthentikUserService(
    private val coreAuthentikApi: CoreAuthentikApi
) {

    /**
     * Retrieves a list of all users from the Authentik
     *
     * This function fetches user data from the API in a paginated manner. It retrieves
     * the first page of users and determines the total number of pages. Then, it iterates
     * through the remaining pages (if any) to collect all user data into a single list.
     *
     * @return A list of all users retrieved from the CoreAuthentik API.
     */
    fun getAllUsers(): List<User> {
        val users = mutableListOf<User>()
        val firstPage = coreAuthentikApi.coreUsersList()
        users.addAll(firstPage.results)
        val totalPages = firstPage.pagination.totalPages?.toInt() ?: 1
        for (i in 2..totalPages) {
            val page = coreAuthentikApi.coreUsersList(page = i)
            users.addAll(page.results)
        }
        return users
    }
}
