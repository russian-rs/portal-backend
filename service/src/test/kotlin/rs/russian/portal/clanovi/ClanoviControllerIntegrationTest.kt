package rs.russian.portal.clanovi

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.security.core.context.SecurityContextHolder
import rs.russian.generated.model.ApplicationDto
import rs.russian.portal.application.repository.ApplicationRepository
import rs.russian.portal.application.service.ApplicationService
import rs.russian.portal.config.DefaultUserFilter
import rs.russian.portal.testconfig.AbstractIntegrationTest
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClanoviControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var applicationService: ApplicationService

    @Autowired
    lateinit var applicationRepository: ApplicationRepository

    @Autowired
    lateinit var defaultUserFilter: DefaultUserFilter

    @BeforeAll
    fun setUpRestAssured(@LocalServerPort port: Int) {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @BeforeEach
    fun setupAuth() {
        SecurityContextHolder.getContext().authentication = defaultUserFilter.getDefaultOAuth2Token()
    }

    @Test
    fun `applications delta requires clanovi key`() {
        Given {
            contentType(ContentType.JSON)
        } When {
            get("/clanovi/applications")
        } Then {
            statusCode(401)
        }
    }

    @Test
    fun `wrong clanovi key is rejected`() {
        Given {
            contentType(ContentType.JSON)
            header(ClanoviApiKeyFilter.HEADER, "not-the-key")
        } When {
            get("/clanovi/applications")
        } Then {
            statusCode(401)
        }
    }

    @Test
    fun `public cities endpoint still works without clanovi key`() {
        Given {
            contentType(ContentType.JSON)
        } When {
            get("/cities")
        } Then {
            statusCode(200)
        }
    }

    @Test
    fun `delta returns only applications newer than since and skips depersonalized`() {
        val older = applicationService.create(
            ApplicationDto(
                id = UUID.randomUUID(),
                email = "clanovi-old-${UUID.randomUUID()}@example.com",
                name = "Older Applicant",
            )
        )
        val newer = applicationService.create(
            ApplicationDto(
                id = UUID.randomUUID(),
                email = "clanovi-new-${UUID.randomUUID()}@example.com",
                name = "Newer Applicant",
            )
        )
        val scrubbed = applicationService.create(
            ApplicationDto(
                id = UUID.randomUUID(),
                email = "clanovi-gone-${UUID.randomUUID()}@example.com",
                name = "Gone Applicant",
            )
        )
        val stored = applicationService.get(scrubbed.id!!)
        stored.depersonalize()
        applicationRepository.save(stored)

        val since = OffsetDateTime.parse("1970-01-01T00:00:00Z").toString()

        Given {
            contentType(ContentType.JSON)
            header(ClanoviApiKeyFilter.HEADER, TEST_KEY)
            queryParam("since", since)
        } When {
            get("/clanovi/applications")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("items.id", hasItem(older.id.toString()))
            body("items.id", hasItem(newer.id.toString()))
            body("items.id", not(hasItem(scrubbed.id.toString())))
            body("truncated", equalTo(false))
        }

        Given {
            contentType(ContentType.JSON)
            header(ClanoviApiKeyFilter.HEADER, TEST_KEY)
            queryParam("since", "2099-01-01T00:00:00Z")
        } When {
            get("/clanovi/applications")
        } Then {
            statusCode(200)
            body("items.size()", equalTo(0))
            body("truncated", equalTo(false))
        }
    }

    @Test
    fun `detail returns anketa and contract shape`() {
        val created = applicationService.create(
            ApplicationDto(
                id = UUID.randomUUID(),
                email = "clanovi-detail-${UUID.randomUUID()}@example.com",
                name = "Detail Applicant",
                phone = "+38161111222",
                telegram = "detail_tg",
            )
        )

        Given {
            contentType(ContentType.JSON)
            header(ClanoviApiKeyFilter.HEADER, TEST_KEY)
        } When {
            get("/clanovi/application/${created.id}")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("id", equalTo(created.id.toString()))
            body("email", equalTo(created.email))
            body("name", equalTo("Detail Applicant"))
            body("phone", equalTo("+38161111222"))
            body("telegram", equalTo("detail_tg"))
        }
    }

    @Test
    fun `open list returns unfinished NEW applications`() {
        val open = applicationService.create(
            ApplicationDto(
                id = UUID.randomUUID(),
                email = "clanovi-open-${UUID.randomUUID()}@example.com",
                name = "Open Applicant",
            )
        )

        Given {
            contentType(ContentType.JSON)
            header(ClanoviApiKeyFilter.HEADER, TEST_KEY)
            queryParam("open", true)
        } When {
            get("/clanovi/applications")
        } Then {
            statusCode(200)
            body("items.id", hasItem(open.id.toString()))
        }
    }

    @Test
    fun `detail of unknown id is 404`() {
        Given {
            contentType(ContentType.JSON)
            header(ClanoviApiKeyFilter.HEADER, TEST_KEY)
        } When {
            get("/clanovi/application/${UUID.randomUUID()}")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `parseSince accepts unix epoch default`() {
        val parsed = ClanoviApplicationService.parseSince(null)
        assertEquals(1970, parsed.year)
        assertTrue(ClanoviApiKeyFilter.keysEqual("abc", "abc"))
        assertTrue(!ClanoviApiKeyFilter.keysEqual("abc", "ab"))
    }

    companion object {
        private const val TEST_KEY = "clanovi-test-key"
    }
}
