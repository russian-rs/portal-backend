package rs.russian.portal.user.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import rs.russian.portal.application.domain.Application
import rs.russian.portal.application.domain.ApplicationStatus
import rs.russian.portal.application.repository.ApplicationRepository
import rs.russian.portal.file.domain.FileInfo
import rs.russian.portal.file.domain.enums.FileExt
import rs.russian.portal.mail.service.EmailService
import rs.russian.portal.note.domain.Note
import rs.russian.portal.note.domain.enums.EntityType
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Contract
import rs.russian.portal.user.domain.ResidencePermit
import rs.russian.portal.user.domain.UserInfo
import rs.russian.portal.user.domain.enums.DepersonalizationStatus
import rs.russian.portal.user.domain.enums.Gender
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.repository.AccountRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class AccountDepersonalizationServiceTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var applicationRepository: ApplicationRepository
    private lateinit var emailService: EmailService
    private lateinit var templateEngine: TemplateEngine
    private lateinit var service: AccountDepersonalizationService

    @BeforeEach
    fun setUp() {
        accountRepository = mockk(relaxed = true)
        applicationRepository = mockk(relaxed = true)
        emailService = mockk(relaxed = true)
        templateEngine = mockk(relaxed = true)
        every { accountRepository.save(any<Account>()) } answers { firstArg() }
        every { applicationRepository.findAllByEmail(any()) } returns emptyList()
        every { templateEngine.process("depersonalization_done_admin", any<Context>()) } returns "body"
        service = AccountDepersonalizationService(
            accountRepository,
            applicationRepository,
            emailService,
            templateEngine,
        )
    }

    @Test
    fun `depersonalize scrubs account identity and role groups`() {
        val account = fullAccount()
        every { accountRepository.findById(account.id!!) } returns Optional.of(account)

        val result = service.depersonalize(account.id!!, emptyList(), THRESHOLD)!!

        assertEquals(AccountDepersonalizationService.DEPERSONALIZED_FULL_NAME, result.fullName)
        assertEquals("depersonalized-${account.id}@deleted.local", result.email)
        assertTrue(result.groups.isEmpty())
        assertEquals(DepersonalizationStatus.DEPERSONALIZED, result.depersonalizationStatus)
        // username is the FK key across tables and is intentionally preserved.
        assertEquals("volunteer", result.username)
    }

    @Test
    fun `depersonalize nulls personal info fields and removes avatar`() {
        val account = fullAccount()
        every { accountRepository.findById(account.id!!) } returns Optional.of(account)

        service.depersonalize(account.id!!, emptyList(), THRESHOLD)

        val info = account.info!!
        assertNull(info.city)
        assertNull(info.postalCode)
        assertNull(info.address)
        assertNull(info.birthDate)
        assertNull(info.telegram)
        assertNull(info.phone)
        assertNull(info.gender)
        assertNull(info.avatar)
    }

    @Test
    fun `depersonalize removes all residence permits`() {
        val account = fullAccount()
        every { accountRepository.findById(account.id!!) } returns Optional.of(account)
        assertTrue(account.residencePermits.isNotEmpty())

        service.depersonalize(account.id!!, emptyList(), THRESHOLD)

        assertTrue(account.residencePermits.isEmpty())
    }

    @Test
    fun `depersonalize persists the scrubbed account`() {
        val account = fullAccount()
        every { accountRepository.findById(account.id!!) } returns Optional.of(account)
        val saved = slot<Account>()
        every { accountRepository.save(capture(saved)) } answers { firstArg() }

        service.depersonalize(account.id!!, emptyList(), THRESHOLD)

        assertEquals(DepersonalizationStatus.DEPERSONALIZED, saved.captured.depersonalizationStatus)
        assertNotNull(saved.captured.depersonalizedAt)
    }

    @Test
    fun `depersonalize enqueues notification to admins with details captured before scrubbing`() {
        val account = fullAccount()
        every { accountRepository.findById(account.id!!) } returns Optional.of(account)
        val admin = admin(10, "admin", "admin@example.com")

        service.depersonalize(account.id!!, listOf(admin), THRESHOLD)

        // Template is rendered with the original full name, not the scrubbed placeholder.
        verify {
            templateEngine.process(
                "depersonalization_done_admin",
                match<Context> { it.getVariable("fullName") == "Ivan Volunteer" }
            )
        }
        verify { emailService.sendCommonEmail(admin, SUBJECT, "body") }
    }

    @Test
    fun `depersonalize does not notify when no admins given`() {
        val account = fullAccount()
        every { accountRepository.findById(account.id!!) } returns Optional.of(account)

        service.depersonalize(account.id!!, emptyList(), THRESHOLD)

        verify(exactly = 0) { templateEngine.process(any<String>(), any<Context>()) }
        verify(exactly = 0) { emailService.sendCommonEmail(any<Account>(), any(), any()) }
    }

    @Test
    fun `depersonalize skips admin with blank email`() {
        val account = fullAccount()
        every { accountRepository.findById(account.id!!) } returns Optional.of(account)
        val adminWithEmail = admin(10, "admin1", "admin1@example.com")
        val adminWithoutEmail = admin(11, "admin2", "")

        service.depersonalize(account.id!!, listOf(adminWithEmail, adminWithoutEmail), THRESHOLD)

        verify(exactly = 1) { emailService.sendCommonEmail(adminWithEmail, SUBJECT, "body") }
        verify(exactly = 0) { emailService.sendCommonEmail(adminWithoutEmail, any(), any()) }
    }

    @Test
    fun `depersonalize skips reanimated account that became active again`() {
        val account = fullAccount()
        account.active = true
        every { accountRepository.findById(account.id!!) } returns Optional.of(account)

        val result = service.depersonalize(account.id!!, emptyList(), THRESHOLD)

        assertNull(result)
        verify(exactly = 0) { accountRepository.save(any<Account>()) }
    }

    @Test
    fun `depersonalize skips account whose status is no longer WARNED`() {
        val account = fullAccount()
        account.depersonalizationStatus = DepersonalizationStatus.NONE
        every { accountRepository.findById(account.id!!) } returns Optional.of(account)

        val result = service.depersonalize(account.id!!, emptyList(), THRESHOLD)

        assertNull(result)
        verify(exactly = 0) { accountRepository.save(any<Account>()) }
    }

    @Test
    fun `depersonalize scrubs the volunteer's applications matched by original email`() {
        val account = fullAccount()
        every { accountRepository.findById(account.id!!) } returns Optional.of(account)
        val application = Application(
            email = "volunteer@example.com",
            name = "Ivan Volunteer",
            status = ApplicationStatus.DONE,
            phone = "+381600000000",
            passport = "1234567890",
            bio = "activist since 2010",
        )
        application.notes.add(
            Note(
                createdBy = "coordinator",
                entityId = application.id!!,
                entityType = EntityType.APPLICATION,
                text = "reviewed Ivan's passport",
            )
        )
        // Matched by the pre-scrub email, not the placeholder written to the account.
        every { applicationRepository.findAllByEmail("volunteer@example.com") } returns listOf(application)

        service.depersonalize(account.id!!, emptyList(), THRESHOLD)

        assertEquals("depersonalized@deleted.local", application.email)
        assertNull(application.passport)
        assertNull(application.phone)
        assertNull(application.bio)
        assertTrue(application.notes.isEmpty())
        // Structural/statistical fields are preserved.
        assertEquals(ApplicationStatus.DONE, application.status)
        verify { applicationRepository.saveAll(listOf(application)) }
    }

    @Test
    fun `depersonalize skips account that vanished before reload`() {
        every { accountRepository.findById(999) } returns Optional.empty()

        val result = service.depersonalize(999, emptyList(), THRESHOLD)

        assertNull(result)
        verify(exactly = 0) { accountRepository.save(any<Account>()) }
    }

    private fun fullAccount(): Account {
        val account = Account(
            id = 1,
            username = "volunteer",
            email = "volunteer@example.com",
            fullName = "Ivan Volunteer",
            active = false,
            groups = setOf(UserGroup.VOLUNTEER),
            depersonalizationStatus = DepersonalizationStatus.WARNED,
        )
        account.info = UserInfo(
            id = account.username,
            account = account,
            city = "Belgrade",
            postalCode = "11000",
            address = "Knez Mihailova 1",
            birthDate = LocalDate.of(1990, 1, 1),
            telegram = "@ivan",
            phone = "+381600000000",
            gender = Gender.MALE,
            avatar = file("avatar", account),
        )
        account.contracts.add(
            Contract(
                account = account,
                startDate = CONTRACT_END_DATE.minusYears(1),
                endDate = CONTRACT_END_DATE,
            )
        )
        account.residencePermits.add(
            ResidencePermit(
                account = account,
                nationality = "RU",
                registrationNumber = "123456789",
                validUntil = LocalDate.of(2025, 1, 1),
                purposeOfStay = "work",
                note = null,
                identityNumber = "1234567890123",
                issuingDate = LocalDate.of(2024, 1, 1),
                issuingAuthority = "MUP",
                stateOfBirth = "RU",
                frontSidePhoto = file("front", account),
                backSidePhoto = file("back", account),
            )
        )
        return account
    }

    private fun admin(id: Int, username: String, email: String) = Account(
        id = id,
        username = username,
        email = email,
        fullName = username,
        active = true,
        lastSynced = LocalDateTime.now(),
    )

    private fun file(id: String, account: Account) = FileInfo(
        id = id,
        name = id,
        size = 1L,
        suffix = FileExt.JPG,
        author = account,
    )

    companion object {
        private const val SUBJECT = "Данные волонтера деперсонализированы"
        private val CONTRACT_END_DATE: LocalDate = LocalDate.now().minusDays(1)
        private val THRESHOLD: LocalDate = LocalDate.now()
    }
}
