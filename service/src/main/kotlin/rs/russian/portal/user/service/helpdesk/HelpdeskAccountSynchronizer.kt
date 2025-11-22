package rs.russian.portal.user.service.helpdesk

import com.helpdesk.model.RoleDto
import com.helpdesk.model.UserDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import rs.russian.portal.ticket.service.HelpdeskApiClient
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.service.AccountSynchronizer

@Service
class HelpdeskAccountSynchronizer(
    private val helpdeskApiClient: HelpdeskApiClient,
) : AccountSynchronizer {

    override fun sync(accounts: List<Account>) {
        try {
            syncRoles(accounts)
            accounts.forEach { syncAccount(it) }
        } catch (e: Throwable) {
            log.error("Error during Helpdesk users sync", e)
        }
    }

    override fun delete(account: Account) {
        var accountEmail = account.email.lowercase()
        try {
            var helpdeskUser = helpdeskApiClient.getUserByEmail(accountEmail)
            if (helpdeskUser == null) {
                return
            }
            helpdeskApiClient.deleteUser(helpdeskUser.id!!)
            log.info("Successfully delete HelpDesk user $accountEmail")
        } catch (e: Throwable) {
            log.error("Error deleting account $accountEmail from HelpDesk", e)
        }
    }

    private fun syncRoles(accounts: List<Account>) {
        val sourceRoles = accounts.flatMap { it.groups }.map { it.oauthGroup.lowercase() }.toSet()
        val existingHelpdeskRoles = helpdeskApiClient.getRoles().map { it.name }.toSet()
        sourceRoles.forEach { sourceRole ->
            if (!existingHelpdeskRoles.contains(sourceRole)) {
                helpdeskApiClient.createRole(RoleDto(name = sourceRole, active = true))
            }
        }
    }

    private fun syncAccount(account: Account) {
        val email = account.email.lowercase()
        try {
            val roles = account.groups
                .map { it.oauthGroup.lowercase() }
                .toMutableSet()
                .also { it.add(DEFAULT_ROLE) }
            val helpdeskUserDto = UserDto(
                login = email,
                email = email,
                firstname = account.fullName.split(" ").first(),
                lastname = account.fullName.split(" ").last(),
                roles = roles
            )
            val helpdeskAccount = helpdeskApiClient.getUserByEmail(email)
            if (helpdeskAccount != null) {
                helpdeskApiClient.updateUser(helpdeskAccount.id!!, helpdeskUserDto)
            } else {
                helpdeskApiClient.createUser(helpdeskUserDto)
            }
        } catch (e: Throwable) {
            log.error("Failed to sync account to HelpDesk $email", e)
        }
    }

    companion object {
        private val DEFAULT_ROLE = "default"
        private val log = LoggerFactory.getLogger(this::class.java)
    }

}
