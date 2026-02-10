package rs.russian.portal.user.service.wordpress

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.wordpress.model.WpUser
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.mapper.WordpressUserMapper

class MultiWordpressAccountSynchronizerTest {

    private lateinit var wordpressUserServices: Map<String, WordpressUserService>
    private lateinit var wordpressUserMapper: WordpressUserMapper
    private lateinit var multiWordpressAccountSynchroniser: MultiWordpressAccountSynchronizer
    private lateinit var mainService: WordpressUserService
    private lateinit var secondaryService: WordpressUserService
    private lateinit var account: Account

    @BeforeEach
    fun setUp() {
        mainService = mockk(relaxed = true)
        secondaryService = mockk(relaxed = true)
        wordpressUserServices = mapOf(
            "main" to mainService,
            "secondary" to secondaryService
        )
        wordpressUserMapper = mockk()
        multiWordpressAccountSynchroniser =
            MultiWordpressAccountSynchronizer(wordpressUserServices, wordpressUserMapper)

        account = mockk()
        every { account.username } returns USERNAME
        every { account.email } returns EMAIL
    }

    @Test
    fun `sync() should sync user to all WordPress instances`() {
        // Arrange
        val updatingWpUser = WpUser(1, USERNAME, EMAIL, mutableListOf())
        val creatingWpUser = WpUser(0, USERNAME, EMAIL, mutableListOf())
        val updatedWpUser = WpUser(1, USERNAME, EMAIL, mutableListOf())

        every { mainService.getUser(EMAIL) } returns updatingWpUser
        every { secondaryService.getUser(EMAIL) } returns null
        every { wordpressUserMapper.map(account) } returns creatingWpUser
        every { wordpressUserMapper.update(account, any()) } returns Unit
        every { mainService.updateUser(updatingWpUser) } returns updatedWpUser
        every { secondaryService.createUser(creatingWpUser) } returns creatingWpUser

        // Act
        multiWordpressAccountSynchroniser.sync(listOf(account))

        // Assert
        verify { mainService.getUser(EMAIL) }
        verify { wordpressUserMapper.update(account, updatingWpUser) }
        verify { mainService.updateUser(updatingWpUser) }

        verify { secondaryService.getUser(EMAIL) }
        verify { wordpressUserMapper.map(account) }
        verify { secondaryService.createUser(creatingWpUser) }
    }

    @Test
    fun `sync() should handle exceptions per instance`() {
        // Arrange
        every { mainService.getUser(EMAIL) } throws RuntimeException("API error")
        every { secondaryService.getUser(EMAIL) } returns null
        every { wordpressUserMapper.map(account) } returns WpUser(0, "", "", mutableListOf())
        every { secondaryService.createUser(any()) } returns mockk()

        // Act - this should not throw even though one service fails
        multiWordpressAccountSynchroniser.sync(listOf(account))

        // Assert
        verify { mainService.getUser(EMAIL) }
        verify { secondaryService.getUser(EMAIL) }
        verify { wordpressUserMapper.map(account) }
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
    }
}
