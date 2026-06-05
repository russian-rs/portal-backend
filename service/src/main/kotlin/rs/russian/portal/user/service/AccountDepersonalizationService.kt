package rs.russian.portal.user.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.enums.DepersonalizationStatus
import rs.russian.portal.user.repository.AccountRepository

/**
 * Performs the irreversible scrubbing of a volunteer's personal data once the retention period has
 * expired. Strips identifying fields from [Account] / [rs.russian.portal.user.domain.UserInfo],
 * deletes residence permits and any attached files (avatar, permit photos cascade to S3 via JPA
 * orphan removal + FileInfoListener), and flips the status to [DepersonalizationStatus.DEPERSONALIZED].
 *
 * Non-personal, statistically relevant data is intentionally kept: contracts (dates/type), program and
 * project. See .tasks/37-depersonalization/Analysis.md for the rationale.
 */
@Service
class AccountDepersonalizationService(
    private val accountRepository: AccountRepository,
) {

    @Transactional
    fun depersonalize(account: Account): Account {
        // Drop residence permits entirely; orphan removal cascades to the photo files and S3.
        account.residencePermits.clear()

        account.info?.let { info ->
            // Nulling the avatar triggers orphan removal -> FileInfoListener removes the S3 object.
            info.avatar = null
            info.city = null
            info.postalCode = null
            info.address = null
            info.birthDate = null
            info.telegram = null
            info.phone = null
            info.gender = null
            // program/project are org reference data, not personal — kept for statistics.
        }

        account.fullName = DEPERSONALIZED_FULL_NAME
        account.email = "depersonalized-${account.id}@deleted.local"
        account.groups = emptySet()
        account.depersonalizationStatus = DepersonalizationStatus.DEPERSONALIZED

        val saved = accountRepository.save(account)
        log.info("Depersonalized account {}", account.username)
        return saved
    }

    companion object {
        private val log = LoggerFactory.getLogger(AccountDepersonalizationService::class.java)
        const val DEPERSONALIZED_FULL_NAME = "Деперсонализированный пользователь"
    }
}
