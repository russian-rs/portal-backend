package rs.russian.portal.user.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rs.russian.portal.file.domain.FileInfo
import rs.russian.portal.file.domain.enums.FileExt
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.ResidencePermit
import rs.russian.portal.user.domain.UserInfo
import rs.russian.portal.user.domain.enums.DepersonalizationStatus
import rs.russian.portal.user.domain.enums.Gender
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.repository.AccountRepository
import java.time.LocalDate

class AccountDepersonalizationServiceTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var service: AccountDepersonalizationService

    @BeforeEach
    fun setUp() {
        accountRepository = mockk(relaxed = true)
        every { accountRepository.save(any<Account>()) } answers { firstArg() }
        service = AccountDepersonalizationService(accountRepository)
    }

    @Test
    fun `depersonalize scrubs account identity and role groups`() {
        val account = fullAccount()

        val result = service.depersonalize(account)

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

        service.depersonalize(account)

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
        assertTrue(account.residencePermits.isNotEmpty())

        service.depersonalize(account)

        assertTrue(account.residencePermits.isEmpty())
    }

    @Test
    fun `depersonalize persists the scrubbed account`() {
        val account = fullAccount()
        val saved = slot<Account>()
        every { accountRepository.save(capture(saved)) } answers { firstArg() }

        service.depersonalize(account)

        assertEquals(DepersonalizationStatus.DEPERSONALIZED, saved.captured.depersonalizationStatus)
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

    private fun file(id: String, account: Account) = FileInfo(
        id = id,
        name = id,
        size = 1L,
        suffix = FileExt.JPG,
        author = account,
    )
}
