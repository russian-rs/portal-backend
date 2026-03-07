package rs.russian.portal.user.scheduler

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import rs.russian.portal.mail.service.EmailService
import rs.russian.portal.user.domain.enums.DepersonalizationStatus
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.repository.AccountRepository
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

@Component
class DepersonalizationWarningScheduler(
    private val accountRepository: AccountRepository,
    private val emailService: EmailService,
    private val templateEngine: TemplateEngine,
    @Value("\${app.depersonalization.warning-period:P4Y10M}")
    private val warningPeriod: Period,
    @Value("\${app.depersonalization.total-period:P5Y}")
    private val totalPeriod: Period,
) {

    @Scheduled(cron = "\${app.schedulers.depersonalization-warning}")
    @SchedulerLock(name = "depersonalizationWarning")
    fun run() {
        val thresholdDate = LocalDate.now().minus(warningPeriod)

        val accounts = accountRepository.findForDepersonalizationWarning(thresholdDate)
        if (accounts.isEmpty()) {
            log.info("No accounts found for depersonalization warning")
            return
        }

        val admins = accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name)
        if (admins.isEmpty()) {
            log.warn("No ADMIN_VOLUNTEER coordinators found, marking accounts as WARNED without sending emails")
        }

        var failures = 0
        accounts.forEach { account ->
            try {
                val contractEndDate = account.contracts.maxOfOrNull { it.endDate } ?: return@forEach
                val depersonalizationDate = contractEndDate.plus(totalPeriod)
                val message = templateEngine.process(
                    "depersonalization_warning_admin",
                    Context().also {
                        it.setVariables(
                            mapOf(
                                "fullName" to account.fullName,
                                "contractEndDate" to contractEndDate.format(DATE_FORMATTER),
                                "depersonalizationDate" to depersonalizationDate.format(DATE_FORMATTER),
                            )
                        )
                    }
                )

                admins.forEach { admin ->
                    if (admin.email.isNotBlank()) {
                        emailService.sendCommonEmail(admin, SUBJECT, message)
                    } else {
                        log.warn("Skipping depersonalization warning for admin {} due to missing email", admin.username)
                    }
                }

                account.depersonalizationStatus = DepersonalizationStatus.WARNED
                accountRepository.save(account)
            } catch (ex: Exception) {
                failures += 1
                log.error("Failed to process depersonalization warning for account {}", account.username, ex)
            }
        }

        log.info("Depersonalization warnings processed: {} (failures: {})", accounts.size - failures, failures,)
    }

    companion object {
        private val log = LoggerFactory.getLogger(DepersonalizationWarningScheduler::class.java)
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private const val SUBJECT = "Предстоящая деперсонализация данных волонтера"
    }
}
