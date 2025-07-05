package rs.russian.portal.user.service.outline

import com.outline.api.GroupsOutlineApi
import com.outline.api.UsersOutlineApi
import com.outline.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.service.AccountSynchroniser
import java.math.BigDecimal
import java.util.*

private val log = LoggerFactory.getLogger(OutlineServiceImpl::class.java)

@Service
class OutlineServiceImpl(
    private val groupsOutlineApi: GroupsOutlineApi,
    private val usersOutlineApi: UsersOutlineApi
) : AccountSynchroniser {
    private val fetchAllLimit = BigDecimal(100_000)

    override fun sync(accounts: List<Account>) {
        try {
            val ourGroups = accounts.flatMap { it.groups }.map { it.oauthGroup }.toSet()

            val groupsListResponse = groupsOutlineApi.groupsList(GroupsListRequest(limit = fetchAllLimit)).data
            val existingOutlineGroups = groupsListResponse?.groups?.toMutableList() ?: mutableListOf()

            val outlineGroups = syncGroupsToOutline(ourGroups, existingOutlineGroups)

            val outlineMemberships = groupsListResponse?.groupMemberships ?: emptyList()

            val outlineUsers = usersOutlineApi.usersList(
                UsersListRequest(
                    filter = UsersListRequest.Filter.active,
                    limit = fetchAllLimit
                )
            ).data ?: emptyList()

            syncUsersToOutlineGroups(accounts, outlineUsers, outlineGroups, outlineMemberships)

            log.info("Outline was synced successfully (${accounts.size} accounts, ${ourGroups.size} groups)")
        } catch (e: Exception) {
            log.error("Error during Outline sync", e)
        }
    }

    private fun syncGroupsToOutline(ourGroups: Set<String>, existingOutlineGroups: Collection<Group>): List<Group> {

        fun addGroupsToOutline(groups: Set<String?>) = groups.filterNotNull().mapNotNull {
            groupsOutlineApi.groupsCreate(GroupsCreateRequest(it)).data
        }

        fun removeGroupsFromOutline(groups: List<Group>) = groups.mapNotNull {
            val id = it.id
            if (id == null) return@mapNotNull null
            groupsOutlineApi.groupsDelete(CollectionsDeleteRequest(id))
            it
        }

        val outlineGroups = existingOutlineGroups.toMutableList()
        val addedGroups = addGroupsToOutline(ourGroups.minus(outlineGroups.map { it.name }))
        outlineGroups.addAll(addedGroups)

        // todo: нужно ли удалять? Думаю нет, т.к. в sync может передаться впоследствии батчами
//        val removedGroups = removeGroupsFromOutline(outlineGroups.filterNot { ourGroups.contains(it.name) } )
//        outlineGroups.removeAll(removedGroups)

        log.info("Synced Outline groups: ${outlineGroups.size}, added: ${addedGroups.size} new groups")
        return outlineGroups
    }

    private fun syncUsersToOutlineGroups(
        ourAccounts: List<Account>,
        outlineUsers: List<User>,
        outlineGroups: List<Group>,
        outlineMemberships: List<GroupMembership>
    ) {
        fun addUsersToGroups(toAdd: Map<UUID, List<User>>) = toAdd.forEach { (groupId, users) ->
            users.forEach { user ->
                groupsOutlineApi.groupsAddUser(
                    GroupsAddUserRequest(
                        id = groupId,
                        userId = user.id!!,
                    )
                )
            }
        }

        fun removeUsersFromGroups(toRemove: Map<UUID, List<User>>) = toRemove.forEach { (groupId, users) ->
            users.forEach { user ->
                groupsOutlineApi.groupsRemoveUser(
                    CollectionsRemoveUserRequest(
                        id = groupId,
                        userId = user.id!!,
                    )
                )
            }
        }

        val outlineUsersByEmail = outlineUsers.associateBy { it.email }

        val shouldBe: Map<UUID, Set<User>> = outlineGroups.associate { group ->
            group.id!! to ourAccounts
                .filter { acc -> acc.groups.any { it.oauthGroup == group.name } }
                .mapNotNull { outlineUsersByEmail[it.email] }
                .toSet()
        }

        val actual: Map<UUID, List<User?>> = outlineMemberships
            .groupBy({ it.groupId!! }, GroupMembership::user)

        val toAdd = shouldBe.mapValues { (groupId, users) ->
            val actualUsers = actual[groupId] ?: emptySet()
            users.minus(actualUsers).filterNotNull()
        }

        addUsersToGroups(toAdd)

        val toRemove = shouldBe.mapValues { (groupId, users) ->
            val actualUsers = actual[groupId] ?: emptySet()
            actualUsers.minus(users).filterNotNull()
        }

        removeUsersFromGroups(toRemove)

        log.info("Synced Outline users to groups: added: ${toAdd.values.sumOf { it.size }} users to ${toAdd.keys.size} groups, removed: ${toRemove.values.sumOf { it.size }} users from ${toRemove.keys.size} groups")
    }

    override fun delete(account: Account) {
        try {
            val outlineUser = usersOutlineApi.usersList(UsersListRequest(emails = mutableListOf(account.email))).data?.firstOrNull()
            if (outlineUser == null) {
                log.warn("Nothing to delete: No Outline user found for email: ${account.email}")
                return
            }

            usersOutlineApi.usersSuspend(UsersInfoRequest(outlineUser.id!!))
            log.info("Successfully suspended Outline user with email: ${account.email}")
        } catch (e: Exception) {
            log.error("Error during Outline users suspend (delete)", e)
        }
    }
}

private data class MiniUser(
    val id: UUID,
    val email: String,
    val name: String?,
)
