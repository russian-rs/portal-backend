package rs.russian.portal.user.service.outline

import com.outline.model.Group
import com.outline.model.User
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.service.AccountSynchroniser
import java.util.*

private val log = LoggerFactory.getLogger(OutlineServiceImpl::class.java)

@Service
@Profile("!local")
class OutlineServiceImpl(
    private val outlineApiClient: OutlineApiClient
) : AccountSynchroniser {

    override fun sync(accounts: List<Account>) {
        try {
            accounts.forEach {
                it.email = it.email.lowercase()
                it.username = it.username.lowercase()
            }

            val ourGroups = accounts.flatMap { it.groups }.map { it.oauthGroup.lowercase() }.toSet()

            val existingOutlineGroups = outlineApiClient.groupsList()
            val outlineMemberships = outlineApiClient.groupMemberships(existingOutlineGroups)

            val outlineGroups = syncGroupsToOutline(ourGroups, existingOutlineGroups)

            val outlineUsers = outlineApiClient.activeUsersList()
            outlineUsers.forEach { user ->
                user.email = user.email?.lowercase()
                user.name = user.name?.lowercase()
            }

            syncUsersToOutlineGroups(accounts, outlineUsers, outlineGroups, outlineMemberships)

            log.info("Outline was synced successfully (${accounts.size} accounts, ${ourGroups.size} groups)")
        } catch (e: Exception) {
            log.error("Error during Outline sync", e)
            // ignore
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
        outlineMemberships: Map<UUID, List<UUID>>
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

        log.info("Synced Outline users to groups: " +
                "added: ${toAdd.values.sumOf { it.size }} users to ${toAdd.keys.size} groups, " +
                "removed: ${toRemove.values.sumOf { it.size }} users from ${toRemove.filterValues { it.isNotEmpty() }.keys.size} groups")
    }

    override fun delete(account: Account) {
        try {
            val outlineUser = outlineApiClient.userByEmail(account.email)
            if (outlineUser == null) {
                log.warn("Nothing to delete: No Outline user found for email: ${account.email}")
                return
            }
            outlineApiClient.usersSuspend(outlineUser.id!!)

            log.info("Successfully suspended Outline user with email: ${account.email}")
        } catch (e: Exception) {
            log.error("Error during Outline users suspend (delete)", e)
            // ignore
        }
    }
}
