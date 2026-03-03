package rs.russian.portal.user.repository

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import rs.russian.portal.testconfig.AbstractIntegrationTest
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.enums.UserGroup
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountRepositoryIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @BeforeEach
    fun setUp() {
        accountRepository.deleteAll()
        accountRepository.flush()
    }

    @Test
    fun `findAllActiveByGroup should return accounts containing specific group`() {
        // Given
        val adminAccount = Account(
            id = 101,
            username = "admin_user",
            email = "admin@example.com",
            fullName = "Admin User",
            groups = setOf(UserGroup.ADMIN, UserGroup.DEVELOPER),
            active = true
        )
        val volunteerAccount = Account(
            id = 102,
            username = "volunteer_user",
            email = "volunteer@example.com",
            fullName = "Volunteer User",
            groups = setOf(UserGroup.VOLUNTEER),
            active = true
        )
        val multiGroupAccount = Account(
            id = 103,
            username = "multi_user",
            email = "multi@example.com",
            fullName = "Multi Group User",
            groups = setOf(UserGroup.VOLUNTEER, UserGroup.TEACHER),
            active = true
        )

        accountRepository.saveAll(listOf(adminAccount, volunteerAccount, multiGroupAccount))
        accountRepository.flush()

        // When - find ADMIN
        val admins = accountRepository.findAllActiveByGroup(UserGroup.ADMIN.name)
        // Then
        assertEquals(1, admins.size)
        assertEquals("admin_user", admins[0].username)

        // When - find VOLUNTEER
        val volunteers = accountRepository.findAllActiveByGroup(UserGroup.VOLUNTEER.name)
        // Then
        assertEquals(2, volunteers.size)
        assertTrue(volunteers.any { it.username == "volunteer_user" })
        assertTrue(volunteers.any { it.username == "multi_user" })

        // When - find TEACHER
        val teachers = accountRepository.findAllActiveByGroup(UserGroup.TEACHER.name)
        // Then
        assertEquals(1, teachers.size)
        assertEquals("multi_user", teachers[0].username)
    }

    @Test
    fun `findAllActiveByGroup should return only active accounts`() {
        // Given
        val activeAdmin = Account(
            id = 201,
            username = "active_admin",
            email = "active@example.com",
            fullName = "Active Admin",
            groups = setOf(UserGroup.ADMIN),
            active = true
        )
        val inactiveAdmin = Account(
            id = 202,
            username = "inactive_admin",
            email = "inactive@example.com",
            fullName = "Inactive Admin",
            groups = setOf(UserGroup.ADMIN),
            active = false
        )

        accountRepository.saveAll(listOf(activeAdmin, inactiveAdmin))
        accountRepository.flush()

        // When
        val result = accountRepository.findAllActiveByGroup(UserGroup.ADMIN.name)

        // Then
        assertEquals(1, result.size)
        assertEquals("active_admin", result[0].username)
    }

    @Test
    fun `findAllActiveByGroup should return empty list when no accounts match`() {
        // Given
        val user = Account(
            id = 301,
            username = "simple_user",
            email = "user@example.com",
            fullName = "User",
            groups = setOf(UserGroup.MEMBER),
            active = true
        )
        accountRepository.save(user)
        accountRepository.flush()

        // When
        val result = accountRepository.findAllActiveByGroup(UserGroup.ADMIN.name)

        // Then
        assertTrue(result.isEmpty())
    }
}
