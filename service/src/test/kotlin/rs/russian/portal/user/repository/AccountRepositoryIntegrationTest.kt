package rs.russian.portal.user.repository

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import rs.russian.portal.report.repository.ReportRepository
import rs.russian.portal.testconfig.AbstractIntegrationTest
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.enums.UserGroup
import kotlin.test.assertEquals
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
}
