import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.application.domain.ApplicationStatus.DENY
import rs.russian.portal.application.domain.ApplicationStatus.DONE
import rs.russian.portal.application.domain.ApplicationType.PROLONGATION
import rs.russian.portal.application.repository.ApplicationRepository
import rs.russian.portal.mail.service.EmailService
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.repository.AccountRepository
import rs.russian.portal.user.service.AccountService
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class ContractExpirationScheduler(
    private val accountRepository: AccountRepository,
    private val applicationRepository: ApplicationRepository,
    private val accountService: AccountService,
    private val emailService: EmailService
) {

    @Scheduled(cron = "\${app.schedulers.contract-expiration}")
    @SchedulerLock(name = "contractExpiration")
    @Transactional
    fun run() {
        val today = LocalDate.now()
        val reminderDate = today.plusDays(REMINDER_DAYS)
        sendExpirationReminders(reminderDate)
        deactivateExpiredAccounts(today)
    }

    private fun sendExpirationReminders(reminderDate: LocalDate) {
        val accounts = accountRepository.findAllWithLatestContractEndDate(reminderDate)
        val notified = accounts.filterNot { hasActiveProlongation(it) }
        notified.forEach { account ->
            val endDate = account.contracts.maxOfOrNull { it.endDate } ?: return@forEach
            val formattedDate = endDate.format(DATE_FORMATTER)
            val deactivationDate = endDate.plusDays(1).format(DATE_FORMATTER)
            if (account.email.isNotBlank()) {
                val message = buildVolunteerReminderMessage(formattedDate, deactivationDate)
                emailService.sendCommonEmail(account, REMINDER_SUBJECT, message)
            } else {
                log.warn("Skipping reminder for account {} due to missing email", account.username)
            }
        }
        log.info("[SCHEDULER] Contract reminders sent: {}", notified.size)
    }

    private fun deactivateExpiredAccounts(today: LocalDate) {
        val accounts = accountRepository.findAllWithLatestContractEndDateOnOrBefore(today.minusDays(1))
        val expired = accounts.filterNot { hasActiveProlongation(it) }
        val admins = accountRepository.findAllActiveByGroup(buildGroupJson(UserGroup.ADMIN_VOLUNTEER))
        expired.forEach { account ->
            account.id?.let { accountService.switchActiveState(it, false) }
            notifyAdminsOfDeactivation(admins, account)
        }
        log.info("[SCHEDULER] Accounts deactivated due to expired contracts: {}", expired.size)
    }

    private fun hasActiveProlongation(account: Account): Boolean {
        val email = account.email
        if (email.isBlank()) {
            return false
        }
        return applicationRepository.existsByEmailAndTypeAndStatusNotIn(email, PROLONGATION, listOf(DONE, DENY))
    }

    private fun buildVolunteerReminderMessage(contractEndDate: String, deactivationDate: String): String {
        return "Ваш контракт заканчивается $contractEndDate. " +
                "Если вы не планируете продлевать его, ничего делать не нужно — " +
                "ваша учетная запись будет деактивирована $deactivationDate."
    }

    private fun notifyAdminsOfDeactivation(admins: List<Account>, account: Account) {
        if (admins.isEmpty()) {
            log.warn("No admins found for deactivation notification")
            return
        }
        val endDate = account.contracts.maxOfOrNull { it.endDate } ?: return
        val formattedDate = endDate.format(DATE_FORMATTER)
        val message = buildAdminDeactivationMessage(account, formattedDate)
        admins.forEach { admin ->
            if (admin.email.isNotBlank()) {
                emailService.sendCommonEmail(admin, DEACTIVATION_SUBJECT, message)
            } else {
                log.warn("Skipping admin notification for account {} due to missing email", admin.username)
            }
        }
    }

    private fun buildAdminDeactivationMessage(account: Account, formattedDate: String): String {
        return "Учетная запись волонтера ${account.fullName} деактивирована. " +
                "Дата окончания контракта: $formattedDate."
    }

    private fun buildGroupJson(group: UserGroup): String {
        return "[\"${group.name}\"]"
    }

    companion object {
        private val log = LoggerFactory.getLogger(ContractExpirationScheduler::class.java)
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private const val REMINDER_DAYS = 7L
        private const val REMINDER_SUBJECT = "Окончание контракта"
        private const val DEACTIVATION_SUBJECT = "Деактивация учетной записи волонтера"
    }
}