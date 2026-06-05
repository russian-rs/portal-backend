package rs.russian.portal.user.scheduler

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import rs.russian.portal.mail.service.EmailService
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.repository.AccountRepository
import rs.russian.portal.user.service.AccountDepersonalizationService
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

/**
 * Executes the actual depersonalization of volunteers whose retention period has expired.
 *
 * Runs daily after [DepersonalizationWarningScheduler]: picks up accounts already flagged WARNED whose
 * last contract ended at least [totalPeriod] ago, scrubs their personal data via
 * [AccountDepersonalizationService], and notifies coordinators.
 */
@Component
class DepersonalizationScheduler(
    private val accountRepository: AccountRepository,
    private val depersonalizationService: AccountDepersonalizationService,
    private val emailService: EmailService,
    private val templateEngine: TemplateEngine,
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
                // Capture identifying details before they are scrubbed, for the notification email.
                val fullName = account.fullName
                val contractEndDate = account.contracts.maxOfOrNull { it.endDate }

                depersonalizationService.depersonalize(account)

                if (admins.isNotEmpty()) {
                    val message = templateEngine.process(
                        "depersonalization_done_admin",
                        Context().also {
                            it.setVariables(
                                mapOf(
                                    "fullName" to fullName,
                                    "contractEndDate" to (contractEndDate?.format(DATE_FORMATTER) ?: "—"),
                                    "depersonalizationDate" to LocalDate.now().format(DATE_FORMATTER),
                                )
                            )
                        }
                    )
                    admins.forEach { admin ->
                        if (admin.email.isNotBlank()) {
                            emailService.sendCommonEmail(admin, SUBJECT, message)
                        } else {
                            log.warn("Skipping depersonalization notice for admin {} due to missing email", admin.username)
                        }
                    }
                }
            } catch (ex: Exception) {
                failures += 1
                log.error("Failed to depersonalize account {}", account.username, ex)
            }
        }

        log.info("Depersonalization processed: {} (failures: {})", accounts.size - failures, failures)
    }

    companion object {
        private val log = LoggerFactory.getLogger(DepersonalizationScheduler::class.java)
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private const val SUBJECT = "Данные волонтера деперсонализированы"
    }
}
