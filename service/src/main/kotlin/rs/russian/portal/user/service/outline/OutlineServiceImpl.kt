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
        // todo wrap all to try

        // groups logic
        val ourGroups = accounts.flatMap { it.groups }.map { it.oauthGroup }.toSet()

        val groupsListResponse = groupsOutlineApi.groupsList(GroupsListRequest(limit = fetchAllLimit)).data
        val outlineGroups = groupsListResponse?.groups?.toMutableList() ?: mutableListOf()

        val addedGroups = ourGroups.minus(outlineGroups.map { it.name })
            .filterNotNull().mapNotNull {
                groupsOutlineApi.groupsCreate(GroupsCreateRequest(it)).data
            }
        outlineGroups.addAll(addedGroups)

        outlineGroups.associate { it.name to it.id }

        val outlineMemberships = groupsListResponse?.groupMemberships ?: emptyList()

        // users logic
        val outlineUsers = usersOutlineApi.usersList(
            UsersListRequest(
                filter = UsersListRequest.Filter.active,
                limit = fetchAllLimit
            )
        ).data ?: emptyList()

        val existingUserEmailsToMiniUser = outlineUsers.associate { it.email to MiniUser(it.id!!, it.email!!, it.name) }

        val shouldBe: Map<UUID, Set<MiniUser>> = outlineGroups.associateWith { group ->
            accounts.filter { acc -> acc.groups.any { it.oauthGroup == group.name } }
                .mapNotNull { existingUserEmailsToMiniUser[it.email] }
        }.mapKeys { it.key.id!! }
            .mapValues { (_, v) -> v.toSet() }

        val actual: Map<UUID, Set<MiniUser>> = outlineMemberships
            .groupBy({ it.groupId!! }, GroupMembership::user)
            .mapValues { (_, accounts) -> accounts.mapNotNull { existingUserEmailsToMiniUser[it?.email] }.toSet() }

        val toDo = shouldBe.mapValues { (groupId, users) ->
            val actualUsers = actual[groupId] ?: emptySet()
            users.minus(actualUsers)
        }

        // sync
        toDo.forEach { (groupId, users) ->
            users.forEach { user ->
                groupsOutlineApi.groupsAddUser(
                    GroupsAddUserRequest(
                        id = groupId,
                        userId = user.id,
                    )
                )
            }
        }

        // remove from groups
        val toRemove = shouldBe.mapValues { (groupId, users) ->
            val actualUsers = actual[groupId] ?: emptySet()
            actualUsers.minus(users)
        }

        toRemove.forEach { (groupId, users) ->
            users.forEach { user ->
                groupsOutlineApi.groupsRemoveUser(
                    CollectionsRemoveUserRequest(
                        id = groupId,
                        userId = user.id,
                    )
                )
            }
        }

    }

    override fun delete(account: Account) {
        // todo wrap all to try
        val outlineUser = usersOutlineApi.usersList(UsersListRequest(emails = mutableListOf(account.email))).data?.firstOrNull()
        if (outlineUser == null) {
            log.warn("Nothing to delete: No Outline user found for email: ${account.email}")
            return
        }

        usersOutlineApi.usersSuspend(UsersInfoRequest(outlineUser.id!!))
        log.info("Successfully suspended Outline user with email: ${account.email}")
    }
}

private data class MiniUser(
    val id: UUID,
    val email: String,
    val name: String?,
)
