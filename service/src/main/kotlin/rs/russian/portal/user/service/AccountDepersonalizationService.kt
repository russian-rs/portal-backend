package rs.russian.portal.user.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import rs.russian.portal.mail.service.EmailService
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.enums.DepersonalizationStatus
import rs.russian.portal.user.repository.AccountRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Performs the irreversible scrubbing of a volunteer's personal data once the retention period has
 * expired. Strips identifying fields from [Account] / [rs.russian.portal.user.domain.UserInfo],
 * deletes residence permits and any attached files (avatar, permit photos cascade to S3 via JPA
 * orphan removal + FileInfoListener), and flips the status to [DepersonalizationStatus.DEPERSONALIZED].
 *
 * Non-personal, statistically relevant data is intentionally kept: contracts (dates/type), program and
 * project. See .tasks/37-depersonalization/Analysis.md for the rationale.
 *
 * Scrubbing and the admin notification run in a single transaction (transactional outbox): the
 * [rs.russian.portal.mail.domain.EmailOutbox] row is persisted in the same transaction as the
 * `DEPERSONALIZED` state change, so the notification is enqueued if and only if the account is actually
 * depersonalized. On any failure the whole transaction rolls back, the account stays `WARNED`, and the
 * next scheduler run retries it — no lost or duplicated notifications. Actual delivery (with retries) is
 * handled separately by the email outbox scheduler.
 */
@Service
class AccountDepersonalizationService(
    private val accountRepository: AccountRepository,
    private val emailService: EmailService,
    private val templateEngine: TemplateEngine,
) {

    @Transactional
    fun depersonalize(account: Account, admins: List<Account>): Account {
        val fullName = account.fullName
        val contractEndDate = account.contracts.maxOfOrNull { it.endDate }

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

        val saved = accountRepository.save(account)

        notifyAdmins(admins, fullName, contractEndDate)

        log.info("Depersonalized account {}", account.username)
        return saved
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
