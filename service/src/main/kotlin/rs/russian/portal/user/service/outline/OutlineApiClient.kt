package rs.russian.portal.user.service.outline

import com.outline.api.GroupsOutlineApi
import com.outline.api.UsersOutlineApi
import com.outline.model.*
import java.math.BigDecimal
import java.util.*

class OutlineApiClient(
    private val groupsOutlineApi: GroupsOutlineApi,
    private val usersOutlineApi: UsersOutlineApi
) {
    private val maxFetchLimit = BigDecimal(100) // Outline API restriction

    fun groupsList() = fetchAll { offset: BigDecimal ->
        groupsOutlineApi.groupsList(GroupsListRequest(offset = offset, limit = maxFetchLimit)).data?.groups
    }

    /**
     * Returns a map group ID -> to list of user IDs that are members of that group.
     */
    fun groupMemberships(groups: List<Group>): Map<UUID, List<UUID>> =
        groups.mapNotNull(Group::id).associateWith { groupId ->
            groupsOutlineApi
                .groupsMemberships(GroupsMembershipsRequest(id = groupId.toString(), limit = maxFetchLimit))
                .data
                ?.groupMemberships
                ?.mapNotNull(GroupMembership::user)
                ?.mapNotNull(User::id) // This endpoint return cropped user objects, so we need to extract IDs
                ?: emptyList()
        }

    fun activeUsersList() = fetchAll {
        usersOutlineApi.usersList(
            UsersListRequest(
                filter = UsersListRequest.Filter.active,
                limit = maxFetchLimit
            )
        ).data
    }

    fun userByEmail(email: String): User? =
        usersOutlineApi.usersList(
            UsersListRequest(
                emails = mutableListOf(email.lowercase()),
                limit = BigDecimal(1)
            )
        ).data?.firstOrNull()

    fun usersSuspend(userId: UUID) = usersOutlineApi.usersSuspend(UsersInfoRequest(userId))

    fun groupsCreate(name: String) = groupsOutlineApi.groupsCreate(GroupsCreateRequest(name)).data

    fun groupsAddUser(groupId: UUID, userId: UUID) =
        groupsOutlineApi.groupsAddUser(GroupsAddUserRequest(groupId, userId))

    fun groupsRemoveUser(groupId: UUID, userId: UUID) =
        groupsOutlineApi.groupsRemoveUser(CollectionsRemoveUserRequest(groupId, userId))

    private fun <T> fetchAll(fetching: (BigDecimal) -> MutableList<T>?): MutableList<T> {
        val items = mutableListOf<T>()
        var offset = BigDecimal(0)
        do {
            val responseItems = fetching(offset) ?: mutableListOf()
            items.addAll(responseItems)
            offset += maxFetchLimit
        } while (responseItems.size >= maxFetchLimit.toInt())

        return items
    }

}
