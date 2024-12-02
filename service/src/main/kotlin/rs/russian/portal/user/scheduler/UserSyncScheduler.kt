package rs.russian.portal.user.scheduler

import io.authentik.model.UserTypeEnum
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import rs.russian.portal.shared.jpa.isNull
import rs.russian.portal.shared.jpa.less
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Account_
import rs.russian.portal.user.mapper.WordpressUserMapper
import rs.russian.portal.user.repository.AccountRepository
import rs.russian.portal.user.service.AccountService
import rs.russian.portal.user.service.AuthentikUserService
import rs.russian.portal.user.service.WordpressUserService
import java.time.LocalDateTime

@Component
class UserSyncScheduler(
    private val accountService: AccountService,
    private val accountRepository: AccountRepository,
    private val wordpressUserMapper: WordpressUserMapper,
    private val wordpressUserService: WordpressUserService,
    private val authentikUserService: AuthentikUserService
) {

    @Scheduled(cron = "0 0 */1 * * *")
    @SchedulerLock(name = "syncUsers")
    fun sync() {
        val syncStart = LocalDateTime.now()
        val authentikUsers = authentikUserService.getAllUsers()
        authentikUsers
            .filter { it.type != UserTypeEnum.service_account }
            .filter { it.type != UserTypeEnum.internal_service_account }
            .filter { !it.email.isNullOrBlank() }
            .forEach { ssoUser ->
                val account = accountService.createOrUpdateAccount(ssoUser)
                createOrUpdateWpUser(account)
            }
        val inactiveSpec = less<Account, LocalDateTime>(Account_.LAST_SYNCED, syncStart)
            .or(isNull(Account_.LAST_SYNCED))
        val inactiveUsers = accountRepository.findAll(inactiveSpec)
        inactiveUsers.forEach { user ->
            accountService.save(user.also { it.active = false })
            deleteWpUserIfExists(user)
        }
    }

    private fun createOrUpdateWpUser(account: Account) {
        try {
            val wpUser = wordpressUserService.getUser(account.username)
            if (wpUser == null) {
                wordpressUserService.createUser(wordpressUserMapper.map(account))
            } else {
                wordpressUserMapper.update(account, wpUser)
                wordpressUserService.updateUser(wpUser)
            }
        } catch (e: Exception) {
            log.error("Failed to create/update WordPress user - ${account.username}", e)
        }
    }

    private fun deleteWpUserIfExists(account: Account) {
        try {
            wordpressUserService.deleteUser(account.username)
        } catch (e: Exception) {
            log.error("Failed to delete WordPress user - ${account.username}", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
