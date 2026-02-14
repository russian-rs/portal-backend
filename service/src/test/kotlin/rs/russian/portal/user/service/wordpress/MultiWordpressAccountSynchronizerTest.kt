package rs.russian.portal.user.service.wordpress

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.wordpress.model.WpRole
import org.wordpress.model.WpUser
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.mapper.WordpressUserMapper
import rs.russian.portal.user.mapper.WordpressUserMapperImpl

class MultiWordpressAccountSynchronizerTest {

    private lateinit var account: Account

    private lateinit var mainService: WordpressUserService

    private lateinit var secondaryService: WordpressUserService

    private lateinit var wordpressUserMapper: WordpressUserMapper

    private lateinit var wordpressUserServices: Map<String, WordpressUserService>

    private lateinit var multiWordpressAccountSynchroniser: MultiWordpressAccountSynchronizer

    @BeforeEach
    fun setUp() {
        mainService = mockk(relaxed = true)
        secondaryService = mockk(relaxed = true)
        wordpressUserServices = mapOf(
            "main" to mainService,
            "secondary" to secondaryService
        )
        wordpressUserMapper = spyk(WordpressUserMapperImpl())
        multiWordpressAccountSynchroniser =
            MultiWordpressAccountSynchronizer(wordpressUserServices, wordpressUserMapper)

        account = spyk(Account(username = USERNAME, fullName = NAME, email = EMAIL))
    }

    @Test
    fun `sync() should sync user to all WordPress instances`() {
        // Arrange
        val updatingWpUser = WpUser(1, USERNAME, EMAIL, mutableListOf())
        val creatingWpUser = WpUser(0, USERNAME, EMAIL, mutableListOf())
        val updatedWpUser = WpUser(1, USERNAME, EMAIL, mutableListOf())

        every { mainService.getUser(EMAIL) } returns updatingWpUser
        every { secondaryService.getUser(EMAIL) } returns null
        every { wordpressUserMapper.create(account, any()) } returns creatingWpUser
        every { wordpressUserMapper.update(account, any(), any()) } returns updatingWpUser
        every { mainService.updateUser(updatingWpUser) } returns updatedWpUser
        every { secondaryService.createUser(creatingWpUser) } returns creatingWpUser

        // Act
        multiWordpressAccountSynchroniser.sync(listOf(account))

        // Assert
        verify { mainService.getUser(EMAIL) }
        verify { wordpressUserMapper.update(account, any(), updatingWpUser) }
        verify { mainService.updateUser(updatingWpUser) }

        verify { secondaryService.getUser(EMAIL) }
        verify { wordpressUserMapper.create(account, any()) }
        verify { secondaryService.createUser(creatingWpUser) }
    }

    @Test
    fun `sync() should sync only wordpress roles`() {
        // Arrange
        val wpUser = WpUser(1, USERNAME, EMAIL, mutableListOf())
        every { mainService.getUser(EMAIL) } returns wpUser
        every { mainService.getAvailableRoles() } returns listOf(
            WpRole(
                slug = UserGroup.TEACHER.oauthGroup,
                name = "Teacher"
            )
        )
        every { account.groups } returns setOf(UserGroup.ADMIN, UserGroup.TEACHER)

        val updatedWpUser = wordpressUserMapper.update(account, setOf(UserGroup.TEACHER.oauthGroup), wpUser)

        // Act
        multiWordpressAccountSynchroniser.sync(listOf(account))

        // Assert
        verify { mainService.updateUser(updatedWpUser) }
    }

    @Test
    fun `sync() should handle exceptions per instance`() {
        // Arrange
        every { mainService.getUser(EMAIL) } throws RuntimeException("API error")
        every { secondaryService.getUser(EMAIL) } returns null
        every { wordpressUserMapper.create(account, any()) } returns WpUser(0, "", "", mutableListOf())
        every { secondaryService.createUser(any()) } returns mockk()

        // Act - this should not throw even though one service fails
        multiWordpressAccountSynchroniser.sync(listOf(account))

        // Assert
        verify { mainService.getUser(EMAIL) }
        verify { secondaryService.getUser(EMAIL) }
        verify { wordpressUserMapper.create(account, any()) }
        verify { secondaryService.createUser(any()) }
    }

    @Test
    fun `delete() should delete user from all WordPress instances`() {
        // Arrange
        every { mainService.deleteUser(EMAIL) } returns Unit
        every { secondaryService.deleteUser(EMAIL) } returns Unit

        // Act
        multiWordpressAccountSynchroniser.delete(account)

        // Assert
        verify { mainService.deleteUser(EMAIL) }
        verify { secondaryService.deleteUser(EMAIL) }
    }

    @Test
    fun `delete() should handle exceptions per instance`() {
        // Arrange
        every { mainService.deleteUser(EMAIL) } throws RuntimeException("API error")
        every { secondaryService.deleteUser(EMAIL) } returns Unit

        // Act - this should not throw even though one service fails
        multiWordpressAccountSynchroniser.delete(account)

        // Assert
        verify { mainService.deleteUser(EMAIL) }
        verify { secondaryService.deleteUser(EMAIL) }
    }

    companion object {
        private const val USERNAME = "testuser"
        private const val EMAIL = "testuser@mail.com"
        private const val NAME = "Test User"
    }
}
