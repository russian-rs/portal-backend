package rs.russian.portal.user.service.wordpress

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.wordpress.api.UsersWordpressApi
import org.wordpress.model.DeleteUser200Response
import org.wordpress.model.WpUser
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WordpressUserServiceImplTest {

    private lateinit var usersWordpressApi: UsersWordpressApi
    private lateinit var wordpressUserService: WordpressUserServiceImpl

    @BeforeEach
    fun setUp() {
        usersWordpressApi = mockk()
        wordpressUserService = WordpressUserServiceImpl(usersWordpressApi)
    }

    @Test
    fun `getUser should return user when found`() {
        // Arrange
        val username = "testuser"
        val wpUser = WpUser(id = 1, username = username, email = "test@example.com")
        every { usersWordpressApi.searchUsers(any(), any()) } returns mutableListOf(wpUser)

        // Act
        val result = wordpressUserService.getUser(username)

        // Assert
        assertEquals(wpUser, result)
        verify { usersWordpressApi.searchUsers(search = "testuser") }
    }

    @Test
    fun `getUser should return null when user not found`() {
        // Arrange
        val username = "testuser"
        every { usersWordpressApi.searchUsers(any(), any()) } returns mutableListOf(
            WpUser(id = 1, username = "otheruser", email = "other@example.com")
        )

        // Act
        val result = wordpressUserService.getUser(username)

        // Assert
        assertNull(result)
        verify { usersWordpressApi.searchUsers(search = "testuser") }
    }

    @Test
    fun `getUser should handle email username format`() {
        // Arrange
        val username = "test@example.com"
        val wpUser = WpUser(id = 1, username = username, email = username)
        every { usersWordpressApi.searchUsers(any(), any()) } returns mutableListOf(wpUser)

        // Act
        val result = wordpressUserService.getUser(username)

        // Assert
        assertEquals(wpUser, result)
        verify { usersWordpressApi.searchUsers(search = "testexample.com") }
    }

    @Test
    fun `createUser should delegate to api client`() {
        // Arrange
        val wpUser = WpUser(id = 0, username = "testuser", email = "test@example.com")
        val createdUser = wpUser.copy(id = 1)
        every { usersWordpressApi.createUser(any()) } returns createdUser

        // Act
        val result = wordpressUserService.createUser(wpUser)

        // Assert
        assertEquals(createdUser, result)
        verify { usersWordpressApi.createUser(wpUser) }
    }

    @Test
    fun `updateUser should delegate to api client`() {
        // Arrange
        val wpUser = WpUser(id = 1, username = "testuser", email = "test@example.com")
        every { usersWordpressApi.updateUser(any(), any()) } returns wpUser

        // Act
        val result = wordpressUserService.updateUser(wpUser)

        // Assert
        assertEquals(wpUser, result)
        verify { usersWordpressApi.updateUser(1, wpUser) }
    }

    @Test
    fun `deleteUser should find user and delete by id`() {
        // Arrange
        val username = "testuser"
        val wpUser = WpUser(id = 1, username = username, email = "test@example.com")
        every { usersWordpressApi.searchUsers(any(), any()) } returns mutableListOf(wpUser)
        every { usersWordpressApi.deleteUser(any()) } returns DeleteUser200Response()

        // Act
        wordpressUserService.deleteUser(username)

        // Assert
        verify { usersWordpressApi.searchUsers(search = "testuser") }
        verify { usersWordpressApi.deleteUser(1) }
    }

    @Test
    fun `deleteUser should do nothing if user not found`() {
        // Arrange
        val username = "testuser"
        every { usersWordpressApi.searchUsers(any(), any()) } returns mutableListOf()

        // Act
        wordpressUserService.deleteUser(username)

        // Assert
        verify { usersWordpressApi.searchUsers(search = "testuser") }
        verify(exactly = 0) { usersWordpressApi.deleteUser(any()) }
    }
}
