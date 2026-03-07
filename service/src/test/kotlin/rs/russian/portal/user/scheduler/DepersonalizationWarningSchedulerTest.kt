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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

class DepersonalizationWarningSchedulerTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var emailService: EmailService
    private lateinit var templateEngine: TemplateEngine
    private lateinit var scheduler: DepersonalizationWarningScheduler

    @BeforeEach
    fun setUp() {
        accountRepository = mockk(relaxed = true)
        emailService = mockk(relaxed = true)
        templateEngine = mockk(relaxed = true)
        scheduler = DepersonalizationWarningScheduler(
            accountRepository,
            emailService,
            templateEngine,
            warningPeriod = WARNING_PERIOD,
            totalPeriod = TOTAL_PERIOD,
        )
    }

    @Test
    fun `run should notify admins and mark accounts as WARNED`() {
        val thresholdDate = LocalDate.now().minus(WARNING_PERIOD)
        val contractEndDate = thresholdDate.minusDays(1)
        val account = createAccount(1, "volunteer", "volunteer@example.com", contractEndDate)
        val admin = createAdmin(10, "admin", "admin@example.com")
        val messageBody = "warning-body"

        every { accountRepository.findForDepersonalizationWarning(thresholdDate) } returns listOf(account)
        every { accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name) } returns listOf(admin)
        every { templateEngine.process("depersonalization_warning_admin", any<Context>()) } returns messageBody

        scheduler.run()

        verify { templateEngine.process("depersonalization_warning_admin", any<Context>()) }
        verify { emailService.sendCommonEmail(admin, SUBJECT, messageBody) }
        verify { accountRepository.save(match { it.depersonalizationStatus == DepersonalizationStatus.WARNED }) }
    }

    @Test
    fun `run should return early when no accounts found`() {
        val thresholdDate = LocalDate.now().minus(WARNING_PERIOD)

        every { accountRepository.findForDepersonalizationWarning(thresholdDate) } returns emptyList()

        scheduler.run()

        verify(exactly = 0) { accountRepository.findAllActiveByGroup(any()) }
        verify(exactly = 0) { emailService.sendCommonEmail(any<Account>(), any(), any()) }
    }

    @Test
    fun `run should mark account as WARNED even when no admins found`() {
        val thresholdDate = LocalDate.now().minus(WARNING_PERIOD)
        val account = createAccount(1, "volunteer", "volunteer@example.com", thresholdDate.minusDays(1))

        every { accountRepository.findForDepersonalizationWarning(thresholdDate) } returns listOf(account)
        every { accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name) } returns emptyList()
        every { templateEngine.process("depersonalization_warning_admin", any<Context>()) } returns "body"

        scheduler.run()

        verify(exactly = 0) { emailService.sendCommonEmail(any<Account>(), any(), any()) }
        verify { accountRepository.save(match { it.depersonalizationStatus == DepersonalizationStatus.WARNED }) }
    }

    @Test
    fun `run should continue processing when one account fails`() {
        val thresholdDate = LocalDate.now().minus(WARNING_PERIOD)
        val account1 = createAccount(1, "volunteer1", "v1@example.com", thresholdDate.minusDays(1))
        val account2 = createAccount(2, "volunteer2", "v2@example.com", thresholdDate.minusDays(2))
        val admin = createAdmin(10, "admin", "admin@example.com")

        every { accountRepository.findForDepersonalizationWarning(thresholdDate) } returns listOf(account1, account2)
        every { accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name) } returns listOf(admin)
        every { templateEngine.process("depersonalization_warning_admin", any<Context>()) } returns "body"
        every { accountRepository.save(account1) } throws RuntimeException("boom")

        scheduler.run()

        verify(exactly = 2) { templateEngine.process("depersonalization_warning_admin", any<Context>()) }
        verify(exactly = 2) { emailService.sendCommonEmail(admin, SUBJECT, any()) }
        verify { accountRepository.save(account1) }
        verify { accountRepository.save(account2) }
    }

    @Test
    fun `run should skip admin with blank email`() {
        val thresholdDate = LocalDate.now().minus(WARNING_PERIOD)
        val account = createAccount(1, "volunteer", "volunteer@example.com", thresholdDate.minusDays(1))
        val adminWithEmail = createAdmin(10, "admin1", "admin1@example.com")
        val adminWithoutEmail = createAdmin(11, "admin2", "")

        every { accountRepository.findForDepersonalizationWarning(thresholdDate) } returns listOf(account)
        every { accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name) } returns listOf(adminWithEmail, adminWithoutEmail)
        every { templateEngine.process("depersonalization_warning_admin", any<Context>()) } returns "body"

        scheduler.run()

        verify(exactly = 1) { emailService.sendCommonEmail(adminWithEmail, SUBJECT, "body") }
        verify(exactly = 0) { emailService.sendCommonEmail(adminWithoutEmail, any(), any()) }
    }

    @Test
    fun `run should notify all admins for each account`() {
        val thresholdDate = LocalDate.now().minus(WARNING_PERIOD)
        val account = createAccount(1, "volunteer", "volunteer@example.com", thresholdDate.minusDays(1))
        val admin1 = createAdmin(10, "admin1", "admin1@example.com")
        val admin2 = createAdmin(11, "admin2", "admin2@example.com")

        every { accountRepository.findForDepersonalizationWarning(thresholdDate) } returns listOf(account)
        every { accountRepository.findAllActiveByGroup(UserGroup.ADMIN_VOLUNTEER.name) } returns listOf(admin1, admin2)
        every { templateEngine.process("depersonalization_warning_admin", any<Context>()) } returns "body"

        scheduler.run()

        verify { emailService.sendCommonEmail(admin1, SUBJECT, "body") }
        verify { emailService.sendCommonEmail(admin2, SUBJECT, "body") }
    }

    private fun createAccount(id: Int, username: String, email: String, contractEndDate: LocalDate): Account {
        val account = Account(
            id = id,
            username = username,
            email = email,
            fullName = username,
            active = false,
            lastSynced = LocalDateTime.now(),
        )
        val contract = Contract(
            account = account,
            startDate = contractEndDate.minusYears(1),
            endDate = contractEndDate,
        )
        account.contracts.add(contract)
        return account
    }

    private fun createAdmin(id: Int, username: String, email: String): Account {
        return Account(
            id = id,
            username = username,
            email = email,
            fullName = username,
            active = true,
            lastSynced = LocalDateTime.now(),
        )
    }

    companion object {
        private val WARNING_PERIOD: Period = Period.of(4, 10, 0)
        private val TOTAL_PERIOD: Period = Period.ofYears(5)
        private const val SUBJECT = "Предстоящая деперсонализация данных волонтера"
    }
}
