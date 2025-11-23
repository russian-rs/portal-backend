package rs.russian.portal.ticket.service

import com.helpdesk.api.RolesHelpdeskApi
import com.helpdesk.api.TicketsHelpdeskApi
import com.helpdesk.api.UsersHelpdeskApi
import com.helpdesk.model.RoleDto
import com.helpdesk.model.TicketDto
import com.helpdesk.model.UserDto

class HelpdeskApiClient(
    private val rolesHelpdeskApi: RolesHelpdeskApi,
    private val usersHelpdeskApi: UsersHelpdeskApi,
    private val ticketsHelpdeskApi: TicketsHelpdeskApi,
) {

    fun getRoles() = rolesHelpdeskApi.getRoles()

    fun createRole(roleDto: RoleDto) = rolesHelpdeskApi.createRole(roleDto)

    fun getUserByEmail(email: String): UserDto? {
        var searchResult = usersHelpdeskApi.searchUser(email)
        if (searchResult.isEmpty()) {
            return null
        }
        return searchResult[0]
    }

    fun createUser(userDto: UserDto) = usersHelpdeskApi.createUser(userDto)

    fun updateUser(id: Int, userDto: UserDto) = usersHelpdeskApi.updateUser(id, userDto)

    fun deleteUser(id: Int) = usersHelpdeskApi.deleteUser(id)

    fun createTicket(ticketDto: TicketDto) = ticketsHelpdeskApi.createTicket(ticketDto)
}
