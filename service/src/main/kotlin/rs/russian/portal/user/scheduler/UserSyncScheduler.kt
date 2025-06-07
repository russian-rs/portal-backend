package rs.russian.portal.user.scheduler

import io.authentik.model.UserTypeEnum
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import rs.russian.portal.shared.jpa.isNull
import rs.russian.portal.shared.jpa.less
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Account_
import rs.russian.portal.user.repository.AccountRepository
import rs.russian.portal.user.service.AccountService
import rs.russian.portal.user.service.authentik.AuthentikService
import rs.russian.portal.user.service.wordpress.MultiWordpressUserService
import java.time.LocalDateTime

@Component
class UserSyncScheduler(
    private val accountService: AccountService,
    private val accountRepository: AccountRepository,
    private val multiWordpressUserService: MultiWordpressUserService,
    private val authentikUserService: AuthentikService
) {

    @Scheduled(cron = "\${app.schedulers.user-sync}")
    @SchedulerLock(name = "syncUsers")
    fun sync() {
        val syncStart = LocalDateTime.now()
        val authentikUsers = authentikUserService.getAllUsers()
        val wpUsersToSync = authentikUsers
            .filter { it.type != UserTypeEnum.service_account }
            .filter { it.type != UserTypeEnum.internal_service_account }
            .filter { !it.email.isNullOrBlank() }
            .filter { it.isActive == true }
            .map { ssoUser ->
                accountService.createOrUpdateAccount(ssoUser)
            }
        multiWordpressUserService.syncToAll(wpUsersToSync)

        val inactiveSpec = less<Account, LocalDateTime>(Account_.LAST_SYNCED, syncStart)
            .or(isNull(Account_.LAST_SYNCED))
        val inactiveUsers = accountRepository.findAll(inactiveSpec)
        inactiveUsers.forEach { user ->
            accountService.save(user.also { it.active = false })
            multiWordpressUserService.deleteFromAll(user)
        }
    }

}
