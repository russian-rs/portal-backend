package rs.russian.portal.report.integration

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.web.server.LocalServerPort
import rs.russian.portal.testconfig.AbstractIntegrationTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatisticsControllerIntegrationTest : AbstractIntegrationTest() {

    @BeforeAll
    fun setUpRestAssured(@LocalServerPort port: Int) {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    /**
     * Out-of-range years used to reach the service and blow up inside `LocalDate.of`, which reports a
     * malformed request as a server fault. `2147483647` is the interesting one: it parses as an `Int` and so
     * gets past deserialization, unlike a value wide enough to fail parsing.
     */
    @ParameterizedTest
    @ValueSource(ints = [0, -1, 1899, 2101, 999999999, 2147483647])
    fun `both statistics endpoints should return 400 for a year outside the supported range`(year: Int) {
        expectStatus("/statistics/$year", 400)
        expectStatus("/statistics/$year/cities", 400)
    }

    @ParameterizedTest
    @ValueSource(ints = [1900, 2026, 2100])
    fun `both statistics endpoints should accept a year on or inside the supported bounds`(year: Int) {
        expectStatus("/statistics/$year", 200)
        expectStatus("/statistics/$year/cities", 200)
    }

    @Test
    fun `both statistics endpoints should return 400 for a non-numeric year`() {
        expectStatus("/statistics/not-a-year", 400)
        expectStatus("/statistics/not-a-year/cities", 400)
    }

    private fun expectStatus(path: String, expected: Int) {
        Given {
            contentType(ContentType.JSON)
        } When {
            get(path)
        } Then {
            statusCode(expected)
        }
    }
}
