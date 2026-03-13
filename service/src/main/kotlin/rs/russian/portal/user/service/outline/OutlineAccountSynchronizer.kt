package rs.russian.portal.user.service.outline

import com.outline.model.Group
import com.outline.model.User
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.service.AccountSynchronizer
import java.util.*

@Service
@Profile("!local")
class OutlineAccountSynchronizer(
    private val outlineApiClient: OutlineApiClient,
) : AccountSynchronizer {

    override fun sync(accounts: List<Account>) {
        try {
            accounts.forEach {
                it.email = it.email.lowercase()
                it.username = it.username.lowercase()
            }

            val sourceGroups = accounts.flatMap { it.groups }.map { it.oauthGroup.lowercase() }.toSet()

            val existingOutlineGroups = outlineApiClient.groupsList()
            val outlineMemberships = outlineApiClient.groupMemberships(existingOutlineGroups)

            val outlineGroups = syncGroupsToOutline(sourceGroups, existingOutlineGroups)

            val outlineUsers = outlineApiClient.activeUsersList()
            outlineUsers.forEach { user ->
                user.email = user.email?.lowercase()
                user.name = user.name?.lowercase()
            }

            syncUsersToOutlineGroups(accounts, outlineUsers, outlineGroups, outlineMemberships)

            log.info("Outline was synced successfully (${accounts.size} accounts, ${sourceGroups.size} groups)")
        } catch (e: Exception) {
            log.error("Error during Outline sync: {}", e.message, e)
        }
    }

    private fun syncGroupsToOutline(ourGroups: Set<String>, existingOutlineGroups: Collection<Group>): List<Group> {

        fun addGroupsToOutline(groups: Set<String?>) = groups.filterNotNull().mapNotNull {
            outlineApiClient.groupsCreate(it)
        }

        val outlineGroups = existingOutlineGroups.toMutableList()
        val addedGroups = addGroupsToOutline(ourGroups.minus(outlineGroups.map { it.name }))
        outlineGroups.addAll(addedGroups)

        log.info("Synced Outline groups: ${outlineGroups.size}, added: ${addedGroups.size} new groups")
        return outlineGroups
    }

    private fun syncUsersToOutlineGroups(
        ourAccounts: List<Account>,
        outlineUsers: List<User>,
        outlineGroups: List<Group>,
        outlineMemberships: Map<UUID, List<UUID>>,
    ) {
        fun addUsersToGroups(toAdd: Map<UUID, Collection<UUID>>) = toAdd.forEach { (groupId, users) ->
            users.forEach { userId ->
                outlineApiClient.groupsAddUser(groupId, userId)
            }
        }

        fun removeUsersFromGroups(toRemove: Map<UUID, Collection<UUID>>) = toRemove.forEach { (groupId, users) ->
            users.forEach { userId ->
                outlineApiClient.groupsRemoveUser(groupId, userId)
            }
        }

        val outlineUsersByEmail = outlineUsers.associateBy { it.email }

        val shouldBe: Map<UUID, Set<UUID>> = outlineGroups.associate { group ->
            group.id!! to ourAccounts
                .filter { acc -> acc.groups.any { it.oauthGroup.equals(group.name, ignoreCase = true) } }
                .mapNotNull { outlineUsersByEmail[it.email] }
                .mapNotNull { it.id }
                .toSet()
        }

        val toAdd = shouldBe.mapValues { (groupId, users) ->
            val actualUsers = outlineMemberships[groupId] ?: emptySet()
            users.minus(actualUsers)
        }

        addUsersToGroups(toAdd)

        val toRemove = shouldBe.mapValues { (groupId, users) ->
            val actualUsers = outlineMemberships[groupId] ?: emptySet()
            actualUsers.minus(users)
        }

        removeUsersFromGroups(toRemove)

        log.info(
            "Synced Outline users to groups: " +
                    "added: ${toAdd.values.sumOf { it.size }} users to ${toAdd.keys.size} groups, " +
                    "removed: ${toRemove.values.sumOf { it.size }} users from ${toRemove.filterValues { it.isNotEmpty() }.keys.size} groups"
        )
    }

    override fun delete(account: Account) {
        var accountEmail = account.email.lowercase()
        try {
            val outlineUser = outlineApiClient.userByEmail(accountEmail)
            if (outlineUser == null) {
                return
            }
            outlineApiClient.usersSuspend(outlineUser.id!!)
        } catch (e: Exception) {
            log.error("Error suspending Outline user $accountEmail: {}", e.message, e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
