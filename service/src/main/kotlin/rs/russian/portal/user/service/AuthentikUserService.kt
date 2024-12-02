package rs.russian.portal.user.service

import io.authentik.api.CoreAuthentikApi
import io.authentik.model.PatchedUserRequest
import io.authentik.model.User
import io.authentik.model.UserRequest
import org.springframework.stereotype.Service
import rs.russian.portal.user.domain.Account

@Service
class AuthentikUserService(
    private val coreAuthentikApi: CoreAuthentikApi
) {

    fun getUser(email: String): User? {
        val pageResult = coreAuthentikApi.coreUsersList(email = email)
        return pageResult.results.find { it.email == email }
    }

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

    fun createUser(username: String, name: String, email: String): User {
        val request = UserRequest(
            username = username,
            name = name,
            email = email
        )
        return coreAuthentikApi.coreUsersCreate(request)
    }

    fun switchActiveState(account: Account, isActive: Boolean) {
        coreAuthentikApi.coreUsersPartialUpdate(account.id!!, PatchedUserRequest(isActive = isActive))
    }
}
