package rs.russian.portal.shared.exception

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.web.server.LocalServerPort
import rs.russian.portal.testconfig.AbstractIntegrationTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GlobalExceptionHandlerIntegrationTest : AbstractIntegrationTest() {

    @BeforeAll
    fun setUpRestAssured(@LocalServerPort port: Int) {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    /**
     * The unparsable body is rejected during argument resolution, so no translation is ever requested and the
     * test stays independent of the AI client.
     */
    @Test
    fun `a body that is not valid JSON should be reported as 400`() {
        Given {
            contentType(ContentType.JSON)
            body("{ this is not json")
        } When {
            post("/translate/serbian")
        } Then {
            statusCode(400)
        }
    }
}
