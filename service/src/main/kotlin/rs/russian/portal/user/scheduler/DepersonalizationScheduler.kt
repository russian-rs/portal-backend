package rs.russian.portal.user.scheduler

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.repository.AccountRepository
import rs.russian.portal.user.service.AccountDepersonalizationService
import java.time.LocalDate
import java.time.Period

/**
 * Executes the actual depersonalization of volunteers whose retention period has expired.
 *
 * Runs daily after [DepersonalizationWarningScheduler]: picks up accounts already flagged WARNED whose
 * last contract ended at least [totalPeriod] ago, then hands each one to [AccountDepersonalizationService],
 * which scrubs the personal data and enqueues the coordinator notification in a single transaction.
 * Per-account failures are isolated so the rest of the batch still completes; failed accounts stay
 * WARNED and are retried on the next run.
 */
@Component
class DepersonalizationScheduler(
    private val accountRepository: AccountRepository,
    private val depersonalizationService: AccountDepersonalizationService,
    @Value("\${app.depersonalization.total-period:P3Y}")
    private val totalPeriod: Period,
) {

    @Scheduled(cron = "\${app.schedulers.depersonalization}")
    @SchedulerLock(name = "depersonalization")
    fun run() {
        val thresholdDate = LocalDate.now().minus(totalPeriod)

        val accounts = accountRepository.findForDepersonalization(thresholdDate)
        if (accounts.isEmpty()) {
            log.info("No accounts found for depersonalization")
            return
        }

        val admins = accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name)
        if (admins.isEmpty()) {
            log.warn("No ADMIN_VOLUNTEER coordinators found, depersonalizing accounts without notifications")
        }

        var failures = 0
        accounts.forEach { account ->
            try {
                depersonalizationService.depersonalize(account, admins)
            } catch (ex: Exception) {
                failures += 1
                log.error("Failed to depersonalize account {}", account.username, ex)
            }
        }

        log.info("Depersonalization processed: {} (failures: {})", accounts.size - failures, failures)
    }

    companion object {
        private val log = LoggerFactory.getLogger(DepersonalizationScheduler::class.java)
    }
}
