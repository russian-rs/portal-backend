package rs.russian.portal.security.jwt

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import rs.russian.portal.security.domain.UserTokenRepository

@Component
class DeleteExpiredTokensScheduler(
    private val transactionTemplate: TransactionTemplate,
    private val userTokenRepository: UserTokenRepository
) {

    /**
     * Deletes all user tokens that have expired before the current date and time.
     */
    @Scheduled(cron = CRON)
    @SchedulerLock(name = "deleteExpiredTokens", lockAtMostFor = LAMF)
    fun run() = transactionTemplate.executeWithoutResult {
        userTokenRepository.deleteAllByValidUntilBefore()
    }

    companion object {
        private const val CRON = "\${app.scheduler.delete-expired-tokens.cron}"
        private const val LAMF = "\${app.scheduler.delete-expired-tokens.lock-at-most-for}"
    }
}
