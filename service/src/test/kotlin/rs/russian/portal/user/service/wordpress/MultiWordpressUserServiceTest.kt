package rs.russian.portal.user.service.wordpress

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.wordpress.model.WpUser
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.mapper.WordpressUserMapper

class MultiWordpressUserServiceTest {

    private lateinit var wordpressUserServices: Map<String, WordpressUserService>
    private lateinit var wordpressUserMapper: WordpressUserMapper
    private lateinit var multiWordpressUserService: MultiWordpressUserService
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
        multiWordpressUserService = MultiWordpressUserService(wordpressUserServices, wordpressUserMapper)
        
        account = mockk()
        every { account.username } returns "testuser"
    }

    @Test
    fun `syncToAll should sync user to all WordPress instances`() {
        // Arrange
        val wpUser1 = mockk<WpUser>()
        val wpUser2 = mockk<WpUser>()
        val updatedWpUser = mockk<WpUser>()
        
        every { mainService.getUser("testuser") } returns wpUser1
        every { secondaryService.getUser("testuser") } returns null
        every { wordpressUserMapper.map(account) } returns wpUser2
        every { wordpressUserMapper.update(account, wpUser1) } returns Unit
        every { mainService.updateUser(wpUser1) } returns updatedWpUser
        every { secondaryService.createUser(wpUser2) } returns wpUser2
        
        // Act
        multiWordpressUserService.syncToAll(account)
        
        // Assert
        verify { mainService.getUser("testuser") }
        verify { wordpressUserMapper.update(account, wpUser1) }
        verify { mainService.updateUser(wpUser1) }
        
        verify { secondaryService.getUser("testuser") }
        verify { wordpressUserMapper.map(account) }
        verify { secondaryService.createUser(wpUser2) }
    }
    
    @Test
    fun `syncToAll should handle exceptions per instance`() {
        // Arrange
        every { mainService.getUser("testuser") } throws RuntimeException("API error")
        every { secondaryService.getUser("testuser") } returns null
        every { wordpressUserMapper.map(account) } returns mockk()
        every { secondaryService.createUser(any()) } returns mockk()
        
        // Act - this should not throw even though one service fails
        multiWordpressUserService.syncToAll(account)
        
        // Assert
        verify { mainService.getUser("testuser") }
        verify { secondaryService.getUser("testuser") }
        verify { wordpressUserMapper.map(account) }
        verify { secondaryService.createUser(any()) }
    }
    
    @Test
    fun `deleteFromAll should delete user from all WordPress instances`() {
        // Arrange
        every { mainService.deleteUser("testuser") } returns Unit
        every { secondaryService.deleteUser("testuser") } returns Unit
        
        // Act
        multiWordpressUserService.deleteFromAll(account)
        
        // Assert
        verify { mainService.deleteUser("testuser") }
        verify { secondaryService.deleteUser("testuser") }
    }
    
    @Test
    fun `deleteFromAll should handle exceptions per instance`() {
        // Arrange
        every { mainService.deleteUser("testuser") } throws RuntimeException("API error")
        every { secondaryService.deleteUser("testuser") } returns Unit
        
        // Act - this should not throw even though one service fails
        multiWordpressUserService.deleteFromAll(account)
        
        // Assert
        verify { mainService.deleteUser("testuser") }
        verify { secondaryService.deleteUser("testuser") }
    }
} 