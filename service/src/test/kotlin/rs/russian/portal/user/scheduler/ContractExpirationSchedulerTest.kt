package rs.russian.portal.user.scheduler

import ContractExpirationScheduler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import rs.russian.portal.application.domain.ApplicationStatus
import rs.russian.portal.application.domain.ApplicationType
import rs.russian.portal.application.repository.ApplicationRepository
import rs.russian.portal.mail.service.EmailService
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Contract
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.repository.AccountRepository
import rs.russian.portal.user.service.AccountService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ContractExpirationSchedulerTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var applicationRepository: ApplicationRepository
    private lateinit var accountService: AccountService
    private lateinit var emailService: EmailService
    private lateinit var templateEngine: TemplateEngine
    private lateinit var scheduler: ContractExpirationScheduler

    @BeforeEach
    fun setUp() {
        accountRepository = mockk(relaxed = true)
        applicationRepository = mockk(relaxed = true)
        accountService = mockk(relaxed = true)
        emailService = mockk(relaxed = true)
        templateEngine = mockk(relaxed = true)
        scheduler = ContractExpirationScheduler(
            accountRepository,
            applicationRepository,
            accountService,
            emailService,
            templateEngine
        )
    }

    @Test
    fun `run should send reminders for expiring contracts without active prolongation`() {
        val today = LocalDate.now()
        val reminderDate = today.plusDays(7)
        val formattedDate = reminderDate.format(DATE_FORMATTER)
        val account = createAccount(1, "volunteer", "volunteer@example.com", reminderDate)
        val reminderBody = "reminder-body-$formattedDate"

        every { accountRepository.findAllWithLatestContractEndDate(reminderDate) } returns listOf(account)
        every { accountRepository.findAllWithLatestContractEndDateOnOrBefore(today.minusDays(1)) } returns emptyList()
        every { accountRepository.findAllActiveByGroup(buildGroupJson(UserGroup.ADMIN_VOLUNTEER)) } returns emptyList()
        every {
            applicationRepository.existsByEmailAndTypeAndStatusNotIn(
                account.email,
                ApplicationType.PROLONGATION,
                listOf(ApplicationStatus.DONE, ApplicationStatus.DENY)
            )
        } returns false
        every { templateEngine.process("contract_expiration_reminder", any<Context>()) } returns reminderBody

        scheduler.run()

        verify { templateEngine.process("contract_expiration_reminder", any<Context>()) }
        verify {
            emailService.sendCommonEmail(
                account,
                "Окончание контракта",
                reminderBody
            )
        }
        verify(exactly = 0) { accountService.switchActiveState(any(), false) }
    }

    @Test
    fun `run should deactivate expired accounts without active prolongation`() {
        val today = LocalDate.now()
        val account = createAccount(2, "expired", "expired@example.com", today.minusDays(1))
        val admin = createAccount(3, "admin", "admin@example.com", today.minusDays(30))
        val adminBody = "admin-body-${account.fullName}"

        every { accountRepository.findAllWithLatestContractEndDate(today.plusDays(7)) } returns emptyList()
        every { accountRepository.findAllWithLatestContractEndDateOnOrBefore(today.minusDays(1)) } returns listOf(account)
        every { accountRepository.findAllActiveByGroup(buildGroupJson(UserGroup.ADMIN_VOLUNTEER)) } returns listOf(admin)
        every {
            applicationRepository.existsByEmailAndTypeAndStatusNotIn(
                account.email,
                ApplicationType.PROLONGATION,
                listOf(ApplicationStatus.DONE, ApplicationStatus.DENY)
            )
        } returns false
        every { templateEngine.process("contract_deactivation_admin", any<Context>()) } returns adminBody

        scheduler.run()

        verify { accountService.switchActiveState(account.id!!, false) }
        verify { templateEngine.process("contract_deactivation_admin", any<Context>()) }
        verify {
            emailService.sendCommonEmail(
                admin,
                "Деактивация учетной записи волонтера",
                adminBody
            )
        }
    }

    private fun createAccount(id: Int, username: String, email: String, endDate: LocalDate): Account {
        val account = Account(
            id = id,
            username = username,
            email = email,
            fullName = username,
            active = true,
            lastSynced = LocalDateTime.now()
        )
        val contract = Contract(
            account = account,
            startDate = endDate.minusMonths(1),
            endDate = endDate
        )
        account.contracts.add(contract)
        return account
    }

    private fun buildGroupJson(group: UserGroup): String {
        return "[\"${group.name}\"]"
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}