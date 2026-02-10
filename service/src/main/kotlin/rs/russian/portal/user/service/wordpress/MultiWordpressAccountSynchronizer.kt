package rs.russian.portal.user.service.wordpress

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.wordpress.model.WpUser
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.mapper.WordpressUserMapper
import rs.russian.portal.user.service.AccountSynchronizer

@Service
class MultiWordpressAccountSynchronizer(
    private val wordpressUserServices: Map<String, WordpressUserService>,
    private val wordpressUserMapper: WordpressUserMapper,
) : AccountSynchronizer {

    override fun sync(accounts: List<Account>) {
        wordpressUserServices.forEach { (_, service) ->
            try {
                val existingRoles = service.getAvailableRoles().map { it.slug }
                val count = accounts.map { createOrUpdateWpUser(service, it, existingRoles) }.count { it }
                log.info("Successfully synced $count of ${accounts.size} users to WordPress instance ${service.instanceName}")
            } catch (ex: Exception) {
                log.error(
                    "Failed to sync users to WordPress instance ${service.instanceName}, skipping instance: {}",
                    ex.message,
                    ex
                )
            }
        }
    }

    private fun createOrUpdateWpUser(
        service: WordpressUserService,
        account: Account,
        existingRoles: List<String>,
    ): Boolean {
        try {
            var wpUser = service.getUser(account.email)
            if (wpUser == null) { // new user
                wpUser = wordpressUserMapper.map(account)
                wpUser.filterAvailableRoles(existingRoles)
                service.createUser(wpUser)
            } else {
                wordpressUserMapper.update(account, wpUser)
                wpUser.filterAvailableRoles(existingRoles)
                service.updateUser(wpUser)
            }
            return true
        } catch (e: Exception) {
            log.error("Failed to sync WordPress user - ${account.username}: {}", e.message, e)
            return false
        }
    }

    private fun WpUser.filterAvailableRoles(existingRoles: List<String>) = this
        .roles
        .retainAll(existingRoles)

    override fun delete(account: Account) {
        wordpressUserServices.forEach { (name, service) ->
            try {
                service.deleteUser(account.email)
            } catch (e: Exception) {
                log.error("Failed to delete user ${account.username} from WordPress instance $name: {}", e.message, e)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
