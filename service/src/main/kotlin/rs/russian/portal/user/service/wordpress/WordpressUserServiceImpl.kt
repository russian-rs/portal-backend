package rs.russian.portal.user.service.wordpress

import com.fasterxml.jackson.databind.ObjectMapper
import org.wordpress.api.CustomWordpressApi
import org.wordpress.api.UsersWordpressApi
import org.wordpress.infrastructure.ClientException
import org.wordpress.infrastructure.ServerException
import org.wordpress.model.WpUser
import rs.russian.portal.shared.exception.InvalidRequestException

class WordpressUserServiceImpl(
    override val instanceName: String,
    override var apiClient: UsersWordpressApi,
    override var customWpApi: CustomWordpressApi,
    private var objectMapper: ObjectMapper,
) : WordpressUserService {

    override fun getAvailableRoles() = customWpApi.rolesList()

    override fun getUser(email: String): WpUser? = wrapApiCall {
        apiClient.searchUsers(search = email).find { it.email == email }
    }

    override fun createUser(user: WpUser): WpUser = wrapApiCall {
        apiClient.createUser(user)
    }

    override fun updateUser(user: WpUser): WpUser = wrapApiCall {
        apiClient.updateUser(user.id, user)
    }

    override fun deleteUser(email: String) {
        wrapApiCall {
            getUser(email)?.let {
                apiClient.deleteUser(it.id)
            }
        }
    }

    private fun <T> wrapApiCall(call: () -> T): T {
        return try {
            call()
        } catch (e: ClientException) {
            throw InvalidRequestException(
                objectMapper.writeValueAsString(
                    mapOf(
                        "status" to e.statusCode,
                        "message" to e.message,
                        "response" to e.response
                    )
                )
            )
        } catch (e: ServerException) {
            throw InvalidRequestException(
                objectMapper.writeValueAsString(
                    mapOf(
                        "status" to e.statusCode,
                        "message" to e.message,
                        "response" to e.response
                    )
                )
            )
        }
    }
}
