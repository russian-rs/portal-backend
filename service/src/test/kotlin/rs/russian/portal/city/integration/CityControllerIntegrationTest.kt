package rs.russian.portal.city.integration

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import rs.russian.portal.city.domain.City
import rs.russian.portal.city.repository.CityRepository

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("local", "no-auth", "test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CityControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var cityRepository: CityRepository

    @BeforeAll
    fun setUpRestAssured() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @BeforeEach
    fun setUp() {
        cityRepository.deleteAll()
        
        val testCities = listOf(
            City(
                id = "belgrade",
                name = "Beograd",
                nameCyrillic = "Белград",
                hasMup = true,
                active = true
            ),
            City(
                id = "novi-sad",
                name = "Novi Sad",
                nameCyrillic = "Нови Сад",
                hasMup = true,
                active = true
            ),
            City(
                id = "nis",
                name = "Niš",
                nameCyrillic = "Ниш",
                hasMup = false,
                active = true
            ),
            City(
                id = "cacak",
                name = "Čačak",
                nameCyrillic = "Чачак",
                hasMup = true,
                active = true
            ),
            City(
                id = "inactive-city",
                name = "Inactive City",
                nameCyrillic = "Неактивни Град",
                hasMup = false,
                active = false
            )
        )
        
        cityRepository.saveAll(testCities)
        cityRepository.flush()
    }

    @Test
    fun `GET all cities should return all active cities ordered by name`() {
        Given {
            contentType(ContentType.JSON)
        } When {
            get("/api/cities")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("size()", equalTo(4))
            body("[0].code", equalTo("belgrade"))
            body("[0].name", equalTo("Beograd"))
            body("[0].nameCyrillic", equalTo("Белград"))
            body("[0].hasMup", equalTo(true))
            body("[1].code", equalTo("nis"))
            body("[1].name", equalTo("Niš"))
            body("[1].nameCyrillic", equalTo("Ниш"))
            body("[1].hasMup", equalTo(false))
            body("[2].code", equalTo("novi-sad"))
            body("[2].name", equalTo("Novi Sad"))
            body("[2].nameCyrillic", equalTo("Нови Сад"))
            body("[3].code", equalTo("cacak"))
            body("[3].name", equalTo("Čačak"))
            body("[3].nameCyrillic", equalTo("Чачак"))
        }
    }

    @Test
    fun `GET all cities should not return inactive cities`() {
        Given {
            contentType(ContentType.JSON)
        } When {
            get("/api/cities")
        } Then {
            statusCode(200)
            body("code", not(hasItem("inactive-city")))
            body("name", not(hasItem("Inactive City")))
        }
    }

    @Test
    fun `GET city by code should return specific city when found`() {
        Given {
            contentType(ContentType.JSON)
        } When {
            get("/api/cities/belgrade")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("code", equalTo("belgrade"))
            body("name", equalTo("Beograd"))
            body("nameCyrillic", equalTo("Белград"))
            body("hasMup", equalTo(true))
        }
    }

    @Test
    fun `GET city by code should return 404 when city not found`() {
        Given {
            contentType(ContentType.JSON)
        } When {
            get("/api/cities/non-existent")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `GET city by code should return inactive city when found`() {
        Given {
            contentType(ContentType.JSON)
        } When {
            get("/api/cities/inactive-city")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("code", equalTo("inactive-city"))
            body("name", equalTo("Inactive City"))
            body("nameCyrillic", equalTo("Неактивни Град"))
            body("hasMup", equalTo(false))
        }
    }

    @Test
    fun `GET search by Latin name should return city when found`() {
        Given {
            contentType(ContentType.JSON)
            queryParam("name", "Beograd")
        } When {
            get("/api/cities/search")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("code", equalTo("belgrade"))
            body("name", equalTo("Beograd"))
            body("nameCyrillic", equalTo("Белград"))
            body("hasMup", equalTo(true))
        }
    }

    @Test
    fun `GET search by Cyrillic name should return city when found`() {
        Given {
            contentType(ContentType.JSON)
            queryParam("name", "Белград")
        } When {
            get("/api/cities/search")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("code", equalTo("belgrade"))
            body("name", equalTo("Beograd"))
            body("nameCyrillic", equalTo("Белград"))
        }
    }

    @Test
    fun `GET search should be case insensitive for Latin names`() {
        Given {
            contentType(ContentType.JSON)
            queryParam("name", "BEOGRAD")
        } When {
            get("/api/cities/search")
        } Then {
            statusCode(200)
            body("code", equalTo("belgrade"))
            body("name", equalTo("Beograd"))
        }
    }

    @Test
    fun `GET search should be case insensitive for Cyrillic names`() {
        Given {
            contentType(ContentType.JSON)
            queryParam("name", "белград")
        } When {
            get("/api/cities/search")
        } Then {
            statusCode(200)
            body("code", equalTo("belgrade"))
            body("nameCyrillic", equalTo("Белград"))
        }
    }

    @Test
    fun `GET search should handle special characters correctly`() {
        Given {
            contentType(ContentType.JSON)
            queryParam("name", "Čačak")
        } When {
            get("/api/cities/search")
        } Then {
            statusCode(200)
            body("code", equalTo("cacak"))
            body("name", equalTo("Čačak"))
            body("nameCyrillic", equalTo("Чачак"))
        }
    }

    @Test
    fun `GET search should return 404 when city not found`() {
        Given {
            contentType(ContentType.JSON)
            queryParam("name", "NonExistent")
        } When {
            get("/api/cities/search")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `GET search should find inactive cities`() {
        Given {
            contentType(ContentType.JSON)
            queryParam("name", "Inactive City")
        } When {
            get("/api/cities/search")
        } Then {
            statusCode(200)
            body("code", equalTo("inactive-city"))
            body("name", equalTo("Inactive City"))
        }
    }

    @Test
    fun `API endpoints should handle URL encoding properly`() {
        Given {
            contentType(ContentType.JSON)
            queryParam("name", "Novi Sad")
        } When {
            get("/api/cities/search")
        } Then {
            statusCode(200)
            body("code", equalTo("novi-sad"))
            body("name", equalTo("Novi Sad"))
        }
    }

    @Test
    fun `full workflow test - get all cities, search by name, get by code`() {
        // Step 1: Get all active cities
        val allCities = Given {
            contentType(ContentType.JSON)
        } When {
            get("/api/cities")
        } Then {
            statusCode(200)
            body("size()", equalTo(4))
        } Extract {
            jsonPath()
        }

        val firstCityCode = allCities.getString("[0].code")
        val firstCityName = allCities.getString("[0].name")

        // Step 2: Search for the first city by name
        Given {
            contentType(ContentType.JSON)
            queryParam("name", firstCityName)
        } When {
            get("/api/cities/search")
        } Then {
            statusCode(200)
            body("code", equalTo(firstCityCode))
            body("name", equalTo(firstCityName))
        }

        // Step 3: Get the same city by code
        Given {
            contentType(ContentType.JSON)
        } When {
            get("/api/cities/$firstCityCode")
        } Then {
            statusCode(200)
            body("code", equalTo(firstCityCode))
            body("name", equalTo(firstCityName))
        }
    }
}