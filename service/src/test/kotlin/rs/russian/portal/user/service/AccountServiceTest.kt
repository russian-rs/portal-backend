package rs.russian.portal.user.service

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import rs.russian.generated.model.PageRequest
import rs.russian.generated.model.UserSearchFilter
import rs.russian.portal.config.DefaultUserFilter


@SpringBootTest
@ActiveProfiles("local", "no-auth", "test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountServiceTest {
    @Autowired
    lateinit var accountService: AccountService

    @Autowired
    lateinit var defaultUserFilter: DefaultUserFilter

    private lateinit var accountLogin: String

    @BeforeAll
    fun setup() {
        SecurityContextHolder.getContext().authentication =
            defaultUserFilter.getDefaultOAuth2Token()

        val account = accountService.findAccountByLogin(DefaultUserFilter.USERNAME)
            ?: throw IllegalStateException("Default user not created")
        accountLogin = account.username

        accountService.setProgram(account.id!!, "IT")
    }


    @Test
    fun `service returns correct page of UserDto with IT user program`() {
        val page = accountService.search(
            query = "default@mail.com",
            pageRequest = PageRequest(
                pageSize = 10,
                pageNumber = 0
            ),
            filter = UserSearchFilter(
                programCodes = mutableSetOf("IT")
            )
        )

        assert(page.totalElements == 1L)
    }

    @Test
    fun `service returns 0 users with MEDIA user program`() {
        val page = accountService.search(
            query = "default@mail.com",
            pageRequest = PageRequest(
                pageSize = 10,
                pageNumber = 0
            ),
            filter = UserSearchFilter(
                programCodes = mutableSetOf("MEDIA")
            )
        )

        assert(page.totalElements == 0L)
    }

}
