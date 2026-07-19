package rs.russian.portal.user.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import rs.russian.portal.application.repository.ApplicationRepository
import rs.russian.portal.mail.service.EmailService
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.enums.DepersonalizationStatus
import rs.russian.portal.user.repository.AccountRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Irreversibly scrubs a volunteer's personal data once the retention period expires: clears identifying
 * fields, deletes residence permits and attached files (cascade to S3), scrubs the volunteer's applications
 * (matched by the original email), and flips status to [DepersonalizationStatus.DEPERSONALIZED]. Non-personal
 * data (contracts, program, project) is kept for statistics — see .tasks/37-depersonalization/Analysis.md.
 *
 * Scrubbing and the admin notification share one transaction (transactional outbox), so the notice is
 * enqueued iff the account is depersonalized; on failure everything rolls back and the next run retries.
 */
@Service
class AccountDepersonalizationService(
    private val accountRepository: AccountRepository,
    private val applicationRepository: ApplicationRepository,
    private val emailService: EmailService,
    private val templateEngine: TemplateEngine,
) {

    /**
     * Returns the scrubbed account, or `null` if it vanished or is no longer eligible.
     */
    @Transactional
    fun depersonalize(accountId: Int, admins: List<Account>, thresholdDate: LocalDate): Account? {
        val account = accountRepository.findById(accountId).orElse(null)
        if (account == null) {
            log.warn("Account {} not found at depersonalization time, skipping", accountId)
            return null
        }
        if (!isStillEligible(account, thresholdDate)) {
            log.info("Account {} no longer eligible for depersonalization, skipping", account.username)
            return null
        }

        val fullName = account.fullName
        val originalEmail = account.email
        val contractEndDate = account.contracts.maxOfOrNull { it.endDate }

        depersonalizeApplications(originalEmail)

        account.residencePermits.clear()

        account.info?.let { info ->
            info.avatar = null
            info.city = null
            info.postalCode = null
            info.address = null
            info.birthDate = null
            info.telegram = null
            info.phone = null
            info.gender = null
        }

        account.fullName = DEPERSONALIZED_FULL_NAME
        account.email = "depersonalized-${account.id}@deleted.local"
        account.groups = emptySet()
        account.depersonalizationStatus = DepersonalizationStatus.DEPERSONALIZED
        account.depersonalizedAt = LocalDateTime.now()

        val saved = accountRepository.save(account)

        notifyAdmins(admins, fullName, contractEndDate)

        log.info("Depersonalized account {}", account.username)
        return saved
    }

    private fun depersonalizeApplications(email: String) {
        val applications = applicationRepository.findAllByEmail(email)
        applications.forEach { it.depersonalize() }
        if (applications.isNotEmpty()) {
            applicationRepository.saveAll(applications)
        }
    }

    private fun isStillEligible(account: Account, thresholdDate: LocalDate): Boolean {
        if (account.active || account.depersonalizationStatus != DepersonalizationStatus.WARNED) {
            return false
        }
        val latestContractEnd = account.contracts.maxOfOrNull { it.endDate }
        return latestContractEnd != null && !latestContractEnd.isAfter(thresholdDate)
    }

    private fun notifyAdmins(admins: List<Account>, fullName: String, contractEndDate: LocalDate?) {
        if (admins.isEmpty()) {
            return
        }
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

    companion object {
        private val log = LoggerFactory.getLogger(AccountDepersonalizationService::class.java)
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private const val SUBJECT = "Данные волонтера деперсонализированы"
        const val DEPERSONALIZED_FULL_NAME = "Деперсонализированный пользователь"
    }
}
