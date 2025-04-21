package rs.russian.portal.user.service.wordpress

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.mapper.WordpressUserMapper

@Service
class MultiWordpressUserService(
    private val wordpressUserServices: Map<String, WordpressUserService>,
    private val wordpressUserMapper: WordpressUserMapper
) {
    fun syncToAll(account: Account) {
        wordpressUserServices.forEach { (name, service) ->
            try {
                val wpUser = service.getUser(account.username)
                if (wpUser == null) {
                    service.createUser(wordpressUserMapper.map(account))
                } else {
                    wordpressUserMapper.update(account, wpUser)
                    service.updateUser(wpUser)
                }
                log.debug("Successfully synced user ${account.username} to WordPress instance $name")
            } catch (e: Exception) {
                log.error("Failed to sync user ${account.username} to WordPress instance $name", e)
            }
        }
    }

    fun deleteFromAll(account: Account) {
        wordpressUserServices.forEach { (name, service) ->
            try {
                service.deleteUser(account.username)
                log.debug("Successfully deleted user ${account.username} from WordPress instance $name")
            } catch (e: Exception) {
                log.error("Failed to delete user ${account.username} from WordPress instance $name", e)
            }
        }
    }

    private fun createOrUpdateWpUser(service: WordpressUserService, account: Account) {
        try {
            val wpUser = service.getUser(account.username)
            if (wpUser == null) {
                service.createUser(wordpressUserMapper.map(account))
            } else {
                wordpressUserMapper.update(account, wpUser)
                service.updateUser(wpUser)
            }
        } catch (e: Exception) {
            log.error("Failed to create/update WordPress user - ${account.username}", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
} 
