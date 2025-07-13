package rs.russian.portal.user.service.outline

import com.outline.api.GroupsOutlineApi
import com.outline.api.UsersOutlineApi
import com.outline.model.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.instancio.Instancio
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import rs.russian.portal.urils.InstancioUtils.Companion.field
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.enums.UserGroup
import java.math.BigDecimal
import java.util.*

class OutlineServiceImplTest {

    private val groupsOutlineApi = mockk<GroupsOutlineApi>()
    private val usersOutlineApi = mockk<UsersOutlineApi>()
    private var outlineService: OutlineServiceImpl = OutlineServiceImpl(groupsOutlineApi, usersOutlineApi)

    /**
     * ### Before
     *
     * | user  | our groups                                  | outline groups               |
     * |-------|---------------------------------------------|------------------------------|
     * | user1 | DEVELOPER(group1), INSIDE_VOLUNTEER(group2) | DEVELOPER(group1)            |
     * | user2 | DEVELOPER(group1), INSIDE_VOLUNTEER(group2) | INSIDE_VOLUNTEER(group2)     |
     *
     *
     * ### After
     *
     * - `our groups` should match `outline groups`
     *
     * So:
     * - `user1` will be **added** to `INSIDE_VOLUNTEER(group2)`
     * - `user2` will be **added** to `DEVELOPER(group1)`
     *
     */
    @Test
    fun `sync existing groups with members`() {
        // Given: our groups in DB for accounts
        val userGroup1 = UserGroup.DEVELOPER
        val userGroup2 = UserGroup.INSIDE_VOLUNTEER

        // Given: accounts from our db
        val accountGroup1 = Instancio.of(Account::class.java)
            .set(field(Account::groups), setOf(userGroup1, userGroup2))
            .create()
        val accountGroup2 = Instancio.of(Account::class.java)
            .set(field(Account::groups), setOf(userGroup1, userGroup2))
            .create()

        // Given: outline groups
        val unneededGroup = Instancio.of(Group::class.java)
            .set(field(Group::memberCount), BigDecimal.valueOf(1))
            .create()
        val group1 = Instancio.of(Group::class.java)
            .set(field(Group::memberCount), BigDecimal.valueOf(1))
            .set(field(Group::name), userGroup1.oauthGroup)
            .create()
        val group2 = Instancio.of(Group::class.java)
            .set(field(Group::memberCount), BigDecimal.valueOf(1))
            .set(field(Group::name), userGroup2.oauthGroup)
            .create()

        // Given: returning users from outline
        val outlineUser1 = Instancio.of(User::class.java)
            .set(field(User::id), UUID.randomUUID())
            .set(field(User::email), accountGroup1.email)
            .set(field(User::isSuspended), false)
            .create()
        val outlineUser2 = Instancio.of(User::class.java)
            .set(field(User::id), UUID.randomUUID())
            .set(field(User::email), accountGroup2.email)
            .set(field(User::isSuspended), false)
            .create()
        every {
            usersOutlineApi.usersList(match {
                it.filter == UsersListRequest.Filter.active
            })
        } returns UsersList200Response(data = mutableListOf(outlineUser1, outlineUser2))

        // Given: outline group memberships
        val unneededGroupMemberships = GroupMembership(
            id = UUID.randomUUID().toString(),
            groupId = unneededGroup.id,
            userId = outlineUser1.id,
            user = Instancio.of(User::class.java).create()
        )
        val group1Memberships = GroupMembership(
            id = UUID.randomUUID().toString(),
            groupId = group1.id,
            userId = outlineUser1.id,
            user = outlineUser1
        )
        val group2Memberships = GroupMembership(
            id = UUID.randomUUID().toString(),
            groupId = group2.id,
            userId = outlineUser2.id,
            user = outlineUser2
        )

        // Given: returning existing outline groups
        val existingOutlineGroup = mutableListOf(unneededGroup, group1, group2)
        every { groupsOutlineApi.groupsList(any()) } returns GroupsList200Response(
            GroupsList200ResponseData(
                groups = existingOutlineGroup,
                groupMemberships = mutableListOf(unneededGroupMemberships, group1Memberships, group2Memberships)
            )
        )

        // Given: just mocking
        every {
            groupsOutlineApi.groupsAddUser(match {
                it.id == group1.id && it.userId == outlineUser2.id
            })
        } returns GroupsAddUser200Response(null)

        // When
        outlineService.sync(listOf(accountGroup1, accountGroup2))

        // Then: verify members added
        verify(exactly = 2) { groupsOutlineApi.groupsAddUser(any()) }
        verify(exactly = 1) {
            groupsOutlineApi.groupsAddUser(match {
                it.id == group1.id && it.userId == outlineUser2.id
            })
        }
        verify(exactly = 1) {
            groupsOutlineApi.groupsAddUser(match {
                it.id == group2.id && it.userId == outlineUser1.id
            })
        }

        // Then: verify no unneeded interactions
        verify(exactly = 0) {
            groupsOutlineApi.groupsCreate(any())
        }
        verify(exactly = 0) {
            groupsOutlineApi.groupsRemoveUser(any())
        }
        verify(exactly = 0) {
            groupsOutlineApi.groupsAddUser(match { it.id == unneededGroup.id })
        }
    }

    @Test
    fun `sync accounts and create new group`() {
        // Given: our groups in DB for accounts
        val userGroup1Existed = UserGroup.DEVELOPER
        val userGroup2ToCreate = UserGroup.INSIDE_VOLUNTEER // to create in outline

        // Given: accounts from our db
        val accountGroup12 = Instancio.of(Account::class.java)
            .set(field(Account::groups), setOf(userGroup1Existed, userGroup2ToCreate))
            .create()

        // Given: outline groups
        val group1 = Instancio.of(Group::class.java)
            .set(field(Group::name), userGroup1Existed.oauthGroup)
            .create()

        // Given: returning users from outline
        val outlineUser1 = Instancio.of(User::class.java)
            .set(field(User::id), UUID.randomUUID())
            .set(field(User::email), accountGroup12.email)
            .set(field(User::isSuspended), false)
            .create()
        every {
            usersOutlineApi.usersList(match {
                it.filter == UsersListRequest.Filter.active
            })
        } returns UsersList200Response(data = mutableListOf(outlineUser1))

        // Given: outline group memberships
        val group1Memberships = GroupMembership(
            id = UUID.randomUUID().toString(),
            groupId = group1.id,
            userId = outlineUser1.id,
            user = outlineUser1
        )

        // Given: returning existing outline groups
        val existingOutlineGroup = mutableListOf(group1)
        every { groupsOutlineApi.groupsList(any()) } returns GroupsList200Response(
            GroupsList200ResponseData(
                groups = existingOutlineGroup,
                groupMemberships = mutableListOf(group1Memberships)
            )
        )

        // Given: returning userGroup2ToCreate created
        val createdGroup = Instancio.of(Group::class.java)
            .set(field(Group::name), userGroup2ToCreate.oauthGroup)
            .create()
        every {
            groupsOutlineApi.groupsCreate(match { it.name == userGroup2ToCreate.oauthGroup })
        } returns GroupsInfo200Response(createdGroup)

        println("Created group: ${createdGroup.name}, id: ${createdGroup.id}")
        println("Group1: ${group1.name}, id: ${group1.id}")
        println("Outline user1: ${outlineUser1.email}, id: ${outlineUser1.id}")
        // When
        outlineService.sync(listOf(accountGroup12))

        // Then: verify new group created and existing group not created again
        verify(exactly = 1) {
            groupsOutlineApi.groupsCreate(match { it.name == userGroup2ToCreate.oauthGroup })
        }
        verify(exactly = 0) {
            groupsOutlineApi.groupsCreate(match { it.name != userGroup2ToCreate.oauthGroup })
        }

        // Then: verify members added to existing group
        verify(exactly = 1) { groupsOutlineApi.groupsAddUser(any()) }
        verify(exactly = 1) {
            groupsOutlineApi.groupsAddUser(match {
                it.id == createdGroup.id && it.userId == outlineUser1.id
            })
        }
    }

    @Test
    fun `sync accounts and delete from group`() {
        // Given: our groups in DB for accounts
        val userGroup1Existed = UserGroup.DEVELOPER
        val userGroup2ToRemoveFrom = UserGroup.INSIDE_VOLUNTEER

        // Given: accounts from our db
        val accountGroup12 = Instancio.of(Account::class.java)
            .set(field(Account::groups), setOf(userGroup1Existed))
            .create()

        // Given: outline groups
        val group1 = Instancio.of(Group::class.java)
            .set(field(Group::name), userGroup1Existed.oauthGroup)
            .create()
        val group2ToRemoveFrom = Instancio.of(Group::class.java)
            .set(field(Group::name), userGroup2ToRemoveFrom.oauthGroup)
            .create()

        // Given: returning users from outline
        val outlineUser1 = Instancio.of(User::class.java)
            .set(field(User::id), UUID.randomUUID())
            .set(field(User::email), accountGroup12.email)
            .set(field(User::isSuspended), false)
            .create()
        every {
            usersOutlineApi.usersList(match {
                it.filter == UsersListRequest.Filter.active
            })
        } returns UsersList200Response(data = mutableListOf(outlineUser1))

        // Given: outline group memberships
        val group1Memberships = GroupMembership(
            id = UUID.randomUUID().toString(),
            groupId = group1.id,
            userId = outlineUser1.id,
            user = outlineUser1
        )
        val group2Memberships = GroupMembership(
            id = UUID.randomUUID().toString(),
            groupId = group2ToRemoveFrom.id,
            userId = outlineUser1.id,
            user = outlineUser1
        )

        // Given: returning existing outline groups
        val existingOutlineGroup = mutableListOf(group1, group2ToRemoveFrom)
        every { groupsOutlineApi.groupsList(any()) } returns GroupsList200Response(
            GroupsList200ResponseData(
                groups = existingOutlineGroup,
                groupMemberships = mutableListOf(group1Memberships, group2Memberships)
            )
        )

        println("group2ToRemoveFrom group: ${group2ToRemoveFrom.name}, id: ${group2ToRemoveFrom.id}")
        println("Group1: ${group1.name}, id: ${group1.id}")
        println("Outline user1: ${outlineUser1.email}, id: ${outlineUser1.id}")
        // When
        outlineService.sync(listOf(accountGroup12))

        // Then: verify members removed from existing group
        verify(exactly = 1) { groupsOutlineApi.groupsRemoveUser(any()) }
        verify(exactly = 1) {
            groupsOutlineApi.groupsRemoveUser(match {
                it.id == group2ToRemoveFrom.id && it.userId == outlineUser1.id
            })
        }
    }

    @Test
    fun `sync should successfully handle empty accounts list`() {
        // Given
        val emptyAccounts = emptyList<Account>()

        every {
            groupsOutlineApi.groupsList(match { it.limit == BigDecimal(100_000) })
        } returns GroupsList200Response(
            GroupsList200ResponseData(
                groups = mutableListOf(),
                groupMemberships = mutableListOf()
            )
        )

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
