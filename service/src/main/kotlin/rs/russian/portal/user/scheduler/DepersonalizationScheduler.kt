package rs.russian.portal.user.scheduler

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import rs.russian.portal.user.domain.enums.DepersonalizationStatus
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.repository.AccountRepository
import rs.russian.portal.user.service.AccountDepersonalizationService
import java.time.LocalDate
import java.time.Period

/**
 * Depersonalizes WARNED volunteers whose last contract ended at least [totalPeriod] ago. Runs daily after
 * [DepersonalizationWarningScheduler]. Per-account failures are isolated and retried on the next run.
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

        val accountIds = accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.WARNED)
        if (accountIds.isEmpty()) {
            log.info("No accounts found for depersonalization")
            return
        }

        val admins = accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name)
        if (admins.isEmpty()) {
            log.warn("No ADMIN_VOLUNTEER coordinators found, depersonalizing accounts without notifications")
        }

        var processed = 0
        var failures = 0
        accountIds.forEach { accountId ->
            try {
                if (depersonalizationService.depersonalize(accountId, admins, thresholdDate) != null) {
                    processed += 1
                }
            } catch (ex: Exception) {
                failures += 1
                log.error("Failed to depersonalize account {}", accountId, ex)
            }
        }

        log.info("Depersonalization processed: {} (failures: {})", processed, failures)
    }

    companion object {
        private val log = LoggerFactory.getLogger(DepersonalizationScheduler::class.java)
    }
}
