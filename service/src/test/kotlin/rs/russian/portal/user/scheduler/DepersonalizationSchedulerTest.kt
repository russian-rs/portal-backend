package rs.russian.portal.user.scheduler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Contract
import rs.russian.portal.user.domain.enums.DepersonalizationStatus
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.repository.AccountRepository
import rs.russian.portal.user.service.AccountDepersonalizationService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

class DepersonalizationSchedulerTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var depersonalizationService: AccountDepersonalizationService
    private lateinit var scheduler: DepersonalizationScheduler

    @BeforeEach
    fun setUp() {
        accountRepository = mockk(relaxed = true)
        depersonalizationService = mockk(relaxed = true)
        scheduler = DepersonalizationScheduler(
            accountRepository,
            depersonalizationService,
            totalPeriod = TOTAL_PERIOD,
        )
    }

    @Test
    fun `run depersonalizes accounts and passes admins for notification`() {
        val thresholdDate = LocalDate.now().minus(TOTAL_PERIOD)
        val account = createAccount(1, "volunteer", "volunteer@example.com", thresholdDate.minusDays(1))
        val admin = createAdmin(10, "admin", "admin@example.com")

        every {
            accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.WARNED)
        } returns listOf(account.id!!)
        every { accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name) } returns listOf(admin)

        scheduler.run()

        verify { depersonalizationService.depersonalize(account.id!!, listOf(admin), thresholdDate) }
    }

    @Test
    fun `run returns early when no accounts found`() {
        val thresholdDate = LocalDate.now().minus(TOTAL_PERIOD)
        every {
            accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.WARNED)
        } returns emptyList()

        scheduler.run()

        verify(exactly = 0) { depersonalizationService.depersonalize(any(), any(), any()) }
        verify(exactly = 0) { accountRepository.findAllActiveByGroup(any()) }
    }

    @Test
    fun `run depersonalizes even when no admins found`() {
        val thresholdDate = LocalDate.now().minus(TOTAL_PERIOD)
        val account = createAccount(1, "volunteer", "volunteer@example.com", thresholdDate.minusDays(1))

        every {
            accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.WARNED)
        } returns listOf(account.id!!)
        every { accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name) } returns emptyList()

        scheduler.run()

        verify { depersonalizationService.depersonalize(account.id!!, emptyList(), thresholdDate) }
    }

    @Test
    fun `run continues processing when one account fails`() {
        val thresholdDate = LocalDate.now().minus(TOTAL_PERIOD)
        val account1 = createAccount(1, "volunteer1", "v1@example.com", thresholdDate.minusDays(1))
        val account2 = createAccount(2, "volunteer2", "v2@example.com", thresholdDate.minusDays(2))
        val admin = createAdmin(10, "admin", "admin@example.com")

        every {
            accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.WARNED)
        } returns listOf(account1.id!!, account2.id!!)
        every { accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name) } returns listOf(admin)
        every {
            depersonalizationService.depersonalize(account1.id!!, listOf(admin), thresholdDate)
        } throws RuntimeException("boom")

        scheduler.run()

        verify { depersonalizationService.depersonalize(account1.id!!, listOf(admin), thresholdDate) }
        verify { depersonalizationService.depersonalize(account2.id!!, listOf(admin), thresholdDate) }
    }

    private fun createAccount(id: Int, username: String, email: String, contractEndDate: LocalDate): Account {
        val account = Account(
            id = id,
            username = username,
            email = email,
            fullName = username,
            active = false,
            depersonalizationStatus = DepersonalizationStatus.WARNED,
            lastSynced = LocalDateTime.now(),
        )
        account.contracts.add(
            Contract(
                account = account,
                startDate = contractEndDate.minusYears(1),
                endDate = contractEndDate,
            )
        )
        return account
    }

    private fun createAdmin(id: Int, username: String, email: String): Account = Account(
        id = id,
        username = username,
        email = email,
        fullName = username,
        active = true,
        lastSynced = LocalDateTime.now(),
    )

    companion object {
        private val TOTAL_PERIOD: Period = Period.ofYears(3)
    }
}
