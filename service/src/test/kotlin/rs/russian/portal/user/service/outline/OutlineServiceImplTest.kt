package rs.russian.portal.user.service.outline

import com.outline.api.GroupsOutlineApi
import com.outline.api.UsersOutlineApi
import com.outline.model.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import rs.russian.portal.user.domain.Account
import java.math.BigDecimal
import java.util.UUID

class OutlineServiceImplTest {

    private val groupsOutlineApi = mockk<GroupsOutlineApi>()
    private val usersOutlineApi = mockk<UsersOutlineApi>()
    private var outlineService: OutlineServiceImpl = OutlineServiceImpl(groupsOutlineApi, usersOutlineApi)

    @Test
    fun `sync should successfully handle empty accounts list`() {
        // Given
        val emptyAccounts = emptyList<Account>()

        every {
            groupsOutlineApi.groupsList(match { it.limit == BigDecimal(100_000) })
        } returns GroupsList200Response(GroupsList200ResponseData(groups = mutableListOf(), groupMemberships = mutableListOf()))

        every {
            usersOutlineApi.usersList(match {
                it.limit == BigDecimal(100_000) &&
                        it.filter == UsersListRequest.Filter.active
            })
        } returns UsersList200Response(data = mutableListOf())

        // When
        outlineService.sync(emptyAccounts)

        // Then
        verify(exactly = 1) { groupsOutlineApi.groupsList(any()) }
        verify(exactly = 0) { groupsOutlineApi.groupsCreate(any()) }
        verify(exactly = 1) { usersOutlineApi.usersList(any()) }
        verify(exactly = 0) { groupsOutlineApi.groupsAddUser(any()) }
        verify(exactly = 0) { groupsOutlineApi.groupsRemoveUser(any()) }
    }

    @Test
    fun `sync should handle API errors gracefully`() {
        // Given
        every { groupsOutlineApi.groupsList(any()) } throws RuntimeException("API Error")

        // When
        assertDoesNotThrow {
            outlineService.sync(emptyList())
        }

        verify(exactly = 1) { groupsOutlineApi.groupsList(any()) }
        verify(exactly = 0) { usersOutlineApi.usersList(any()) } // groupsOutlineApi failed -> no iteration over users
    }
}
