package rs.russian.portal.city.repository

import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import rs.russian.portal.city.domain.City
import rs.russian.portal.testconfig.AbstractIntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ActiveProfiles("local", "no-auth", "test")
class CityRepositoryIntegrationTest: AbstractIntegrationTest() {

    @Autowired
    private lateinit var cityRepository: CityRepository

    @BeforeEach
    fun setUp() {
        // Clean up any existing data first
        cityRepository.deleteAll()
        cityRepository.flush()
        
        val testCities = listOf(
            City(
                code = "belgrade",
                name = "Beograd",
                nameCyrillic = "Белград",
                hasMup = true,
                active = true
            ),
            City(
                code = "novi-sad",
                name = "Novi Sad",
                nameCyrillic = "Нови Сад",
                hasMup = true,
                active = true
            ),
            City(
                code = "nis",
                name = "Niš",
                nameCyrillic = "Ниш",
                hasMup = true,
                active = true
            ),
            City(
                code = "inactive-city",
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
    fun `findAllActive should return only active cities ordered by name`() {
        val activeCities = cityRepository.findAllActive()
        
        assertEquals(3, activeCities.size)
        assertEquals("Beograd", activeCities[0].name)
        assertEquals("Niš", activeCities[1].name)
        assertEquals("Novi Sad", activeCities[2].name)
        
        activeCities.forEach { city ->
            assertTrue(city.active)
        }
    }

    @Test
    fun `findByNameOrNameCyrillic should find city by Latin name`() {
        val cities = cityRepository.findByNameOrNameCyrillic("Beograd")
        
        assertEquals(1, cities.size)
        val city = cities[0]
        assertEquals("belgrade", city.code)
        assertEquals("Beograd", city.name)
        assertEquals("Белград", city.nameCyrillic)
    }

    @Test
    fun `findByNameOrNameCyrillic should find city by Cyrillic name`() {
        val cities = cityRepository.findByNameOrNameCyrillic("Белград")
        
        assertEquals(1, cities.size)
        val city = cities[0]
        assertEquals("belgrade", city.code)
        assertEquals("Beograd", city.name)
        assertEquals("Белград", city.nameCyrillic)
    }

    @Test
    fun `findByNameOrNameCyrillic should be case insensitive`() {
        // Latin name
        var cities = cityRepository.findByNameOrNameCyrillic("BEOGRAD")
        
        assertEquals(1, cities.size)
        var city = cities[0]
        assertEquals("belgrade", city.code)
        assertEquals("Beograd", city.name)

        // Cyrillic name
        cities = cityRepository.findByNameOrNameCyrillic("белград")
        
        assertEquals(1, cities.size)
        city = cities[0]
        assertEquals("belgrade", city.code)
        assertEquals("Белград", city.nameCyrillic)
    }

    @Test
    fun `findByNameOrNameCyrillic should return empty list when city not found`() {
        val cities = cityRepository.findByNameOrNameCyrillic("NonExistent")
        
        assertTrue(cities.isEmpty())
    }

    @Test
    fun `findByNameOrNameCyrillic should not find inactive city because of active filter`() {
        val cities = cityRepository.findByNameOrNameCyrillic("Inactive City")
        
        assertTrue(cities.isEmpty())
    }

    @Test
    fun `findById should find city by code`() {
        val city = cityRepository.findById("belgrade").orElse(null)
        
        assertNotNull(city)
        assertEquals("Beograd", city.name)
        assertEquals("Белград", city.nameCyrillic)
        assertTrue(city.hasMup)
    }

    @Test
    fun `findById should return empty when code not found`() {
        val city = cityRepository.findById("non-existent").orElse(null)
        
        assertNull(city)
    }

    @Test
    fun `repository operations work correctly with database`() {
        cityRepository.deleteAll()

        val testCity = City(
            code = "test-belgrade",
            name = "Test Beograd",
            nameCyrillic = "Тест Белград",
            hasMup = true,
            active = true
        )

        cityRepository.save(testCity)

        val savedCity = cityRepository.findById("test-belgrade").orElse(null)
        assertNotNull(savedCity)
        assertEquals("Test Beograd", savedCity.name)
        assertEquals("Тест Белград", savedCity.nameCyrillic)
        assertTrue(savedCity.hasMup)
        assertTrue(savedCity.active)

        val foundByName = cityRepository.findByNameOrNameCyrillic("Test Beograd")
        assertEquals(1, foundByName.size)
        assertEquals("test-belgrade", foundByName[0].code)

        val foundByCyrillic = cityRepository.findByNameOrNameCyrillic("Тест Белград")
        assertEquals(1, foundByCyrillic.size)
        assertEquals("test-belgrade", foundByCyrillic[0].code)

        val caseInsensitive = cityRepository.findByNameOrNameCyrillic("TEST BEOGRAD")
        assertEquals(1, caseInsensitive.size)
        assertEquals("test-belgrade", caseInsensitive[0].code)

        val notFound = cityRepository.findByNameOrNameCyrillic("NonExistent")
        assertTrue(notFound.isEmpty())

        val activeCities = cityRepository.findAllActive()
        assertTrue(activeCities.any { it.code == "test-belgrade" })

        val inactiveCity = City(
            code = "inactive-test",
            name = "Inactive Test",
            nameCyrillic = "Неактивни Тест",
            hasMup = false,
            active = false
        )
        cityRepository.save(inactiveCity)

        val allActiveCities = cityRepository.findAllActive()
        assertTrue(allActiveCities.none { it.code == "inactive-test" })
        assertTrue(allActiveCities.any { it.code == "test-belgrade" })
    }
}
