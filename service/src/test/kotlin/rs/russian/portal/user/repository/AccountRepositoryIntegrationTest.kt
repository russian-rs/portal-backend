package rs.russian.portal.user.repository

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import rs.russian.portal.report.repository.ReportRepository
import rs.russian.portal.testconfig.AbstractIntegrationTest
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Contract
import rs.russian.portal.user.domain.UserInfo
import rs.russian.portal.user.domain.enums.DepersonalizationStatus
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.repository.projections.CityVolunteerCountProjection
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountRepositoryIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var reportRepository: ReportRepository

    @BeforeEach
    fun setUp() {
        cleanup()
    }

    @AfterEach
    fun tearDown() {
        cleanup()
    }

    private fun cleanup() {
        val uniqueAccounts = accountRepository.findAll().filter { it.username.endsWith("_unique") }
        uniqueAccounts.forEach {
            accountRepository.delete(it)
        }
        accountRepository.flush()
    }

    @Test
    fun `findAllActiveByGroup should return accounts containing specific group`() {
        // Given
        val adminAccount = Account(
            id = 10101,
            username = "admin_user_unique",
            email = "admin_unique@example.com",
            fullName = "Admin User",
            groups = setOf(UserGroup.ADMIN, UserGroup.DEVELOPER),
            active = true
        )
        val volunteerAccount = Account(
            id = 10102,
            username = "volunteer_user_unique",
            email = "volunteer_unique@example.com",
            fullName = "Volunteer User",
            groups = setOf(UserGroup.VOLUNTEER),
            active = true
        )
        val multiGroupAccount = Account(
            id = 10103,
            username = "multi_user_unique",
            email = "multi_unique@example.com",
            fullName = "Multi Group User",
            groups = setOf(UserGroup.VOLUNTEER, UserGroup.TEACHER),
            active = true
        )

        accountRepository.saveAll(listOf(adminAccount, volunteerAccount, multiGroupAccount))
        accountRepository.flush()

        // When - find ADMIN
        val admins =
            accountRepository.findAllActiveByGroup(UserGroup.ADMIN.name).filter { it.username.endsWith("_unique") }
        // Then
        assertEquals(1, admins.size)
        assertEquals("admin_user_unique", admins[0].username)

        // When - find VOLUNTEER
        val volunteers =
            accountRepository.findAllActiveByGroup(UserGroup.VOLUNTEER.name).filter { it.username.endsWith("_unique") }
        // Then
        assertEquals(2, volunteers.size)
        assertTrue(volunteers.any { it.username == "volunteer_user_unique" })
        assertTrue(volunteers.any { it.username == "multi_user_unique" })

        // When - find TEACHER
        val teachers =
            accountRepository.findAllActiveByGroup(UserGroup.TEACHER.name).filter { it.username.endsWith("_unique") }
        // Then
        assertEquals(1, teachers.size)
        assertEquals("multi_user_unique", teachers[0].username)
    }

    @Test
    fun `findAllActiveByGroup should return only active accounts`() {
        // Given
        val activeAdmin = Account(
            id = 10201,
            username = "active_admin_unique",
            email = "active_unique@example.com",
            fullName = "Active Admin",
            groups = setOf(UserGroup.ADMIN),
            active = true
        )
        val inactiveAdmin = Account(
            id = 10202,
            username = "inactive_admin_unique",
            email = "inactive_unique@example.com",
            fullName = "Inactive Admin",
            groups = setOf(UserGroup.ADMIN),
            active = false
        )

        accountRepository.saveAll(listOf(activeAdmin, inactiveAdmin))
        accountRepository.flush()

        // When
        val result =
            accountRepository.findAllActiveByGroup(UserGroup.ADMIN.name).filter { it.username.endsWith("_unique") }

        // Then
        assertEquals(1, result.size)
        assertEquals("active_admin_unique", result[0].username)
    }

    @Test
    fun `findAllActiveByGroup should return empty list when no accounts match`() {
        // Given
        val user = Account(
            id = 10301,
            username = "simple_user_unique",
            email = "user_unique@example.com",
            fullName = "User",
            groups = setOf(UserGroup.MEMBER),
            active = true
        )
        accountRepository.save(user)
        accountRepository.flush()

        // When
        val result =
            accountRepository.findAllActiveByGroup(UserGroup.ADMIN.name).filter { it.username.endsWith("_unique") }

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findForDepersonalizationWarning should return inactive accounts with contract end before threshold`() {
        // Given
        val thresholdDate = LocalDate.of(2021, 5, 1)
        val account = Account(
            id = 10401,
            username = "depers_warn_unique",
            email = "depers_unique@example.com",
            fullName = "Depers User",
            active = false,
            depersonalizationStatus = DepersonalizationStatus.NONE,
        )
        val contract = Contract(
            account = account,
            startDate = LocalDate.of(2019, 1, 1),
            endDate = LocalDate.of(2021, 4, 30),
        )
        account.contracts.add(contract)
        accountRepository.save(account)
        accountRepository.flush()

        // When
        val result = accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.NONE)

        // Then
        assertTrue(result.contains(10401))
    }

    @Test
    fun `findForDepersonalizationWarning should not return active accounts`() {
        // Given
        val thresholdDate = LocalDate.of(2021, 5, 1)
        val account = Account(
            id = 10501,
            username = "active_depers_unique",
            email = "active_depers_unique@example.com",
            fullName = "Active Depers User",
            active = true,
            depersonalizationStatus = DepersonalizationStatus.NONE,
        )
        val contract = Contract(
            account = account,
            startDate = LocalDate.of(2019, 1, 1),
            endDate = LocalDate.of(2021, 4, 30),
        )
        account.contracts.add(contract)
        accountRepository.save(account)
        accountRepository.flush()

        // When
        val result = accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.NONE)

        // Then
        assertFalse(result.contains(10501))
    }

    @Test
    fun `findForDepersonalizationWarning should not return already warned accounts`() {
        // Given
        val thresholdDate = LocalDate.of(2021, 5, 1)
        val account = Account(
            id = 10601,
            username = "warned_depers_unique",
            email = "warned_depers_unique@example.com",
            fullName = "Warned User",
            active = false,
            depersonalizationStatus = DepersonalizationStatus.WARNED,
        )
        val contract = Contract(
            account = account,
            startDate = LocalDate.of(2019, 1, 1),
            endDate = LocalDate.of(2021, 4, 30),
        )
        account.contracts.add(contract)
        accountRepository.save(account)
        accountRepository.flush()

        // When — querying for NONE accounts must not pick up a WARNED one
        val result = accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.NONE)

        // Then
        assertFalse(result.contains(10601))
    }

    @Test
    fun `findForDepersonalizationWarning should not return accounts with contract end after threshold`() {
        // Given
        val thresholdDate = LocalDate.of(2021, 5, 1)
        val account = Account(
            id = 10701,
            username = "future_depers_unique",
            email = "future_depers_unique@example.com",
            fullName = "Future User",
            active = false,
            depersonalizationStatus = DepersonalizationStatus.NONE,
        )
        val contract = Contract(
            account = account,
            startDate = LocalDate.of(2019, 1, 1),
            endDate = LocalDate.of(2021, 5, 2),
        )
        account.contracts.add(contract)
        accountRepository.save(account)
        accountRepository.flush()

        // When
        val result = accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.NONE)

        // Then
        assertFalse(result.contains(10701))
    }

    @Test
    fun `findForDepersonalizationWarning should use max contract end date when multiple contracts exist`() {
        // Given
        val thresholdDate = LocalDate.of(2022, 6, 1)
        val account = Account(
            id = 10801,
            username = "multi_contract_unique",
            email = "multi_contract_unique@example.com",
            fullName = "Multi Contract User",
            active = false,
            depersonalizationStatus = DepersonalizationStatus.NONE,
        )
        val oldContract = Contract(
            account = account,
            startDate = LocalDate.of(2019, 1, 1),
            endDate = LocalDate.of(2020, 1, 1),
        )
        val recentContract = Contract(
            account = account,
            startDate = LocalDate.of(2021, 1, 1),
            endDate = LocalDate.of(2022, 7, 1),
        )
        account.contracts.addAll(listOf(oldContract, recentContract))
        accountRepository.save(account)
        accountRepository.flush()

        // When — threshold is before the latest contract end, so should NOT match
        val result = accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.NONE)

        // Then
        assertFalse(result.contains(10801))

        // When — threshold after the latest contract end, should match
        val resultAfter =
            accountRepository.findDepersonalizationCandidateIds(LocalDate.of(2022, 7, 2), DepersonalizationStatus.NONE)

        // Then
        assertTrue(resultAfter.contains(10801))
    }

    @Test
    fun `findForDepersonalization should return inactive WARNED accounts with contract end before threshold`() {
        // Given
        val thresholdDate = LocalDate.of(2021, 5, 1)
        val account = Account(
            id = 11001,
            username = "depers_exec_unique",
            email = "depers_exec_unique@example.com",
            fullName = "Depers Exec User",
            active = false,
            depersonalizationStatus = DepersonalizationStatus.WARNED,
        )
        account.contracts.add(
            Contract(
                account = account,
                startDate = LocalDate.of(2019, 1, 1),
                endDate = LocalDate.of(2021, 4, 30),
            )
        )
        accountRepository.save(account)
        accountRepository.flush()

        // When
        val result = accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.WARNED)

        // Then
        assertTrue(result.contains(11001))
    }

    @Test
    fun `findForDepersonalization should not return accounts that were not warned`() {
        // Given
        val thresholdDate = LocalDate.of(2021, 5, 1)
        val noneAccount = Account(
            id = 11101,
            username = "depers_none_unique",
            email = "depers_none_unique@example.com",
            fullName = "Not Warned User",
            active = false,
            depersonalizationStatus = DepersonalizationStatus.NONE,
        )
        val doneAccount = Account(
            id = 11102,
            username = "depers_done_unique",
            email = "depers_done_unique@example.com",
            fullName = "Already Depersonalized User",
            active = false,
            depersonalizationStatus = DepersonalizationStatus.DEPERSONALIZED,
        )
        listOf(noneAccount, doneAccount).forEach { acc ->
            acc.contracts.add(
                Contract(
                    account = acc,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2021, 4, 30),
                )
            )
        }
        accountRepository.saveAll(listOf(noneAccount, doneAccount))
        accountRepository.flush()

        // When
        val result = accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.WARNED)

        // Then
        assertFalse(result.contains(11101))
        assertFalse(result.contains(11102))
    }

    @Test
    fun `findForDepersonalization should not return active accounts`() {
        // Given
        val thresholdDate = LocalDate.of(2021, 5, 1)
        val account = Account(
            id = 11201,
            username = "depers_active_unique",
            email = "depers_active_unique@example.com",
            fullName = "Reactivated User",
            active = true,
            depersonalizationStatus = DepersonalizationStatus.WARNED,
        )
        account.contracts.add(
            Contract(
                account = account,
                startDate = LocalDate.of(2019, 1, 1),
                endDate = LocalDate.of(2021, 4, 30),
            )
        )
        accountRepository.save(account)
        accountRepository.flush()

        // When
        val result = accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.WARNED)

        // Then
        assertFalse(result.contains(11201))
    }

    @Test
    fun `findForDepersonalization should not return accounts with contract end after threshold`() {
        // Given
        val thresholdDate = LocalDate.of(2021, 5, 1)
        val account = Account(
            id = 11301,
            username = "depers_future_unique",
            email = "depers_future_unique@example.com",
            fullName = "Future Exec User",
            active = false,
            depersonalizationStatus = DepersonalizationStatus.WARNED,
        )
        account.contracts.add(
            Contract(
                account = account,
                startDate = LocalDate.of(2019, 1, 1),
                endDate = LocalDate.of(2021, 5, 2),
            )
        )
        accountRepository.save(account)
        accountRepository.flush()

        // When
        val result = accountRepository.findDepersonalizationCandidateIds(thresholdDate, DepersonalizationStatus.WARNED)

        // Then
        assertFalse(result.contains(11301))
    }

    @Test
    fun `countVolunteersByCity should resolve the dictionary by either latin or cyrillic name`() {
        saveVolunteerWithCity(12001, "city_latin_unique", "Novi Sad")
        saveVolunteerWithCity(12002, "city_cyrillic_unique", "Нови Сад")
        saveVolunteerWithCity(12003, "city_lowercase_unique", "novi sad")

        val rows = countVolunteersByCityIn2025()

        assertEquals(1, rows.size)
        assertEquals("novi-sad", rows[0].cityCode)
        assertEquals("Novi Sad", rows[0].cityName)
        assertEquals("Нови Сад", rows[0].cityNameCyrillic)
        assertEquals(3, rows[0].volunteerCount)
    }

    @Test
    fun `countVolunteersByCity should normalize surrounding whitespace and serbian diacritics`() {
        saveVolunteerWithCity(12101, "city_diacritic_unique", "Niš")
        saveVolunteerWithCity(12102, "city_no_diacritic_unique", "Nis")
        saveVolunteerWithCity(12103, "city_padded_unique", "  NIS  ")

        val rows = countVolunteersByCityIn2025()

        assertEquals(1, rows.size)
        assertEquals("nis", rows[0].cityCode)
        assertEquals(3, rows[0].volunteerCount)
    }

    @Test
    fun `countVolunteersByCity should fold case for non-ascii names`() {
        // Guards the UTF-8 ctype dependency: under locale C these would not fold and would land in the roll-up row.
        saveVolunteerWithCity(12701, "city_upper_cyrillic_unique", "  БЕЛГРАД ")
        saveVolunteerWithCity(12702, "city_upper_diacritic_unique", "NIŠ")

        val rows = countVolunteersByCityIn2025()

        assertEquals(listOf("belgrade", "nis"), rows.map { it.cityCode })
        assertEquals(listOf(1, 1), rows.map { it.volunteerCount })
    }

    @Test
    fun `countVolunteersByCity should collapse every value outside the dictionary into one row`() {
        saveVolunteerWithCity(12201, "city_known_unique", "Beograd")
        saveVolunteerWithCity(12202, "city_legacy_a_unique", "Zemun")
        saveVolunteerWithCity(12203, "city_legacy_b_unique", "Podgorica")
        saveVolunteerWithCity(12204, "city_legacy_c_unique", "Zemun")

        val rows = countVolunteersByCityIn2025()

        assertEquals(2, rows.size)
        assertEquals("belgrade", rows[0].cityCode)
        assertEquals(1, rows[0].volunteerCount)
        assertNull(rows[1].cityCode)
        assertNull(rows[1].cityName)
        assertNull(rows[1].cityNameCyrillic)
        assertEquals(3, rows[1].volunteerCount)
    }

    @Test
    fun `countVolunteersByCity should skip volunteers with a blank or missing city`() {
        saveVolunteerWithCity(12301, "city_filled_unique", "Beograd")
        saveVolunteerWithCity(12302, "city_null_unique", null)
        saveVolunteerWithCity(12303, "city_blank_unique", "   ")

        val rows = countVolunteersByCityIn2025()

        assertEquals(1, rows.size)
        assertEquals("belgrade", rows[0].cityCode)
        assertEquals(1, rows[0].volunteerCount)
    }

    @Test
    fun `countVolunteersByCity should only count volunteers whose contract overlaps the year`() {
        saveVolunteerWithCity(
            12401, "city_inside_year_unique", "Beograd",
            contractStart = LocalDate.of(2025, 6, 1), contractEnd = LocalDate.of(2025, 7, 1)
        )
        saveVolunteerWithCity(
            12402, "city_spanning_year_unique", "Beograd",
            contractStart = LocalDate.of(2024, 1, 1), contractEnd = LocalDate.of(2026, 1, 1)
        )
        saveVolunteerWithCity(
            12403, "city_ends_on_jan_first_unique", "Beograd",
            contractStart = LocalDate.of(2024, 1, 1), contractEnd = LocalDate.of(2025, 1, 1)
        )
        saveVolunteerWithCity(
            12404, "city_before_year_unique", "Novi Sad",
            contractStart = LocalDate.of(2023, 1, 1), contractEnd = LocalDate.of(2024, 12, 31)
        )
        saveVolunteerWithCity(
            12405, "city_after_year_unique", "Novi Sad",
            contractStart = LocalDate.of(2026, 1, 1), contractEnd = LocalDate.of(2026, 12, 31)
        )

        val rows = countVolunteersByCityIn2025()

        assertEquals(1, rows.size)
        assertEquals("belgrade", rows[0].cityCode)
        assertEquals(3, rows[0].volunteerCount)
    }

    @Test
    fun `countVolunteersByCity should count a volunteer once regardless of contract count`() {
        val account = volunteerAccount(12501, "city_two_contracts_unique", "Beograd")
        account.contracts.addAll(
            listOf(
                Contract(
                    account = account,
                    startDate = LocalDate.of(2025, 1, 1),
                    endDate = LocalDate.of(2025, 6, 1),
                ),
                Contract(
                    account = account,
                    startDate = LocalDate.of(2025, 7, 1),
                    endDate = LocalDate.of(2025, 12, 31),
                ),
            )
        )
        accountRepository.save(account)
        accountRepository.flush()

        val rows = countVolunteersByCityIn2025()

        assertEquals(1, rows.size)
        assertEquals(1, rows[0].volunteerCount)
    }

    @Test
    fun `countVolunteersByCity should order by count descending but pin the roll-up row last`() {
        saveVolunteerWithCity(12601, "city_order_a_unique", "Novi Sad")
        saveVolunteerWithCity(12602, "city_order_b_unique", "Beograd")
        saveVolunteerWithCity(12603, "city_order_c_unique", "Beograd")
        saveVolunteerWithCity(12604, "city_order_d_unique", "Zemun")
        saveVolunteerWithCity(12605, "city_order_e_unique", "Zemun")
        saveVolunteerWithCity(12606, "city_order_f_unique", "Zemun")

        val rows = countVolunteersByCityIn2025()

        assertEquals(listOf("belgrade", "novi-sad", null), rows.map { it.cityCode })
        assertEquals(listOf(2, 1, 3), rows.map { it.volunteerCount })
    }

    /**
     * Asserting exact row sets is safe even though the query spans the whole table: the seeded database has
     * no volunteers, [cleanup] drops every `_unique` account, and no other test class fills in `user_info.city`.
     */
    private fun countVolunteersByCityIn2025(): List<CityVolunteerCountProjection> =
        accountRepository.countVolunteersByCity(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))

    private fun volunteerAccount(id: Int, username: String, city: String?): Account {
        val account = Account(
            id = id,
            username = username,
            email = "$username@example.com",
            fullName = "Volunteer $id",
            groups = setOf(UserGroup.VOLUNTEER),
            active = true,
        )
        account.info = UserInfo.default(account).apply { this.city = city }
        return account
    }

    private fun saveVolunteerWithCity(
        id: Int,
        username: String,
        city: String?,
        contractStart: LocalDate = LocalDate.of(2025, 1, 1),
        contractEnd: LocalDate = LocalDate.of(2025, 12, 31),
    ) {
        val account = volunteerAccount(id, username, city)
        account.contracts.add(
            Contract(
                account = account,
                startDate = contractStart,
                endDate = contractEnd,
            )
        )
        accountRepository.save(account)
        accountRepository.flush()
    }
}
