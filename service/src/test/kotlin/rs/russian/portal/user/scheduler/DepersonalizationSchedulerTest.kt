package rs.russian.portal.user.scheduler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import rs.russian.portal.mail.service.EmailService
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
    private lateinit var emailService: EmailService
    private lateinit var templateEngine: TemplateEngine
    private lateinit var scheduler: DepersonalizationScheduler

    @BeforeEach
    fun setUp() {
        accountRepository = mockk(relaxed = true)
        depersonalizationService = mockk(relaxed = true)
        emailService = mockk(relaxed = true)
        templateEngine = mockk(relaxed = true)
        scheduler = DepersonalizationScheduler(
            accountRepository,
            depersonalizationService,
            emailService,
            templateEngine,
            totalPeriod = TOTAL_PERIOD,
        )
    }

    @Test
    fun `run depersonalizes accounts and notifies admins`() {
        val thresholdDate = LocalDate.now().minus(TOTAL_PERIOD)
        val account = createAccount(1, "volunteer", "volunteer@example.com", thresholdDate.minusDays(1))
        val admin = createAdmin(10, "admin", "admin@example.com")

        every { accountRepository.findForDepersonalization(thresholdDate) } returns listOf(account)
        every { accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name) } returns listOf(admin)
        every { templateEngine.process("depersonalization_done_admin", any<Context>()) } returns "body"

        scheduler.run()

        verify { depersonalizationService.depersonalize(account) }
        verify { emailService.sendCommonEmail(admin, SUBJECT, "body") }
    }

    @Test
    fun `run returns early when no accounts found`() {
        val thresholdDate = LocalDate.now().minus(TOTAL_PERIOD)
        every { accountRepository.findForDepersonalization(thresholdDate) } returns emptyList()

        scheduler.run()

        verify(exactly = 0) { depersonalizationService.depersonalize(any()) }
        verify(exactly = 0) { accountRepository.findAllActiveByGroup(any()) }
    }

    @Test
    fun `run depersonalizes even when no admins found`() {
        val thresholdDate = LocalDate.now().minus(TOTAL_PERIOD)
        val account = createAccount(1, "volunteer", "volunteer@example.com", thresholdDate.minusDays(1))

        every { accountRepository.findForDepersonalization(thresholdDate) } returns listOf(account)
        every { accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name) } returns emptyList()

        scheduler.run()

        verify { depersonalizationService.depersonalize(account) }
        verify(exactly = 0) { emailService.sendCommonEmail(any<Account>(), any(), any()) }
    }

    @Test
    fun `run continues processing when one account fails`() {
        val thresholdDate = LocalDate.now().minus(TOTAL_PERIOD)
        val account1 = createAccount(1, "volunteer1", "v1@example.com", thresholdDate.minusDays(1))
        val account2 = createAccount(2, "volunteer2", "v2@example.com", thresholdDate.minusDays(2))
        val admin = createAdmin(10, "admin", "admin@example.com")

        every { accountRepository.findForDepersonalization(thresholdDate) } returns listOf(account1, account2)
        every { accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name) } returns listOf(admin)
        every { templateEngine.process("depersonalization_done_admin", any<Context>()) } returns "body"
        every { depersonalizationService.depersonalize(account1) } throws RuntimeException("boom")

        scheduler.run()

        verify { depersonalizationService.depersonalize(account1) }
        verify { depersonalizationService.depersonalize(account2) }
        verify(exactly = 1) { emailService.sendCommonEmail(admin, SUBJECT, "body") }
    }

    @Test
    fun `run skips admin with blank email`() {
        val thresholdDate = LocalDate.now().minus(TOTAL_PERIOD)
        val account = createAccount(1, "volunteer", "volunteer@example.com", thresholdDate.minusDays(1))
        val adminWithEmail = createAdmin(10, "admin1", "admin1@example.com")
        val adminWithoutEmail = createAdmin(11, "admin2", "")

        every { accountRepository.findForDepersonalization(thresholdDate) } returns listOf(account)
        every { accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name) } returns
            listOf(adminWithEmail, adminWithoutEmail)
        every { templateEngine.process("depersonalization_done_admin", any<Context>()) } returns "body"

        scheduler.run()

        verify(exactly = 1) { emailService.sendCommonEmail(adminWithEmail, SUBJECT, "body") }
        verify(exactly = 0) { emailService.sendCommonEmail(adminWithoutEmail, any(), any()) }
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
        private const val SUBJECT = "Данные волонтера деперсонализированы"
    }
}
