package rs.russian.portal.city.repository

import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import rs.russian.portal.city.domain.City
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("local", "no-auth", "test")
class CityRepositoryIntegrationTest {

    @Autowired
    private lateinit var cityRepository: CityRepository

    @BeforeEach
    fun setUp() {
        // Clean up any existing data first
        cityRepository.deleteAll()
        cityRepository.flush()
        
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
        val city = cityRepository.findByNameOrNameCyrillic("Beograd")
        
        assertNotNull(city)
        assertEquals("belgrade", city.id)
        assertEquals("Beograd", city.name)
        assertEquals("Белград", city.nameCyrillic)
    }

    @Test
    fun `findByNameOrNameCyrillic should find city by Cyrillic name`() {
        val city = cityRepository.findByNameOrNameCyrillic("Белград")
        
        assertNotNull(city)
        assertEquals("belgrade", city.id)
        assertEquals("Beograd", city.name)
        assertEquals("Белград", city.nameCyrillic)
    }

    @Test
    fun `findByNameOrNameCyrillic should be case insensitive`() {
        // Latin name
        var city = cityRepository.findByNameOrNameCyrillic("BEOGRAD")
        
        assertNotNull(city)
        assertEquals("belgrade", city.id)
        assertEquals("Beograd", city.name)

        // Cyrillic name
        city = cityRepository.findByNameOrNameCyrillic("белград")
        
        assertNotNull(city)
        assertEquals("belgrade", city.id)
        assertEquals("Белград", city.nameCyrillic)
    }

    @Test
    fun `findByNameOrNameCyrillic should return null when city not found`() {
        val city = cityRepository.findByNameOrNameCyrillic("NonExistent")
        
        assertNull(city)
    }

    @Test
    fun `findByNameOrNameCyrillic should find inactive city`() {
        val city = cityRepository.findByNameOrNameCyrillic("Inactive City")
        
        assertNotNull(city)
        assertEquals("inactive-city", city.id)
        assertEquals(false, city.active)
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
            id = "test-belgrade",
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
        assertNotNull(foundByName)
        assertEquals("test-belgrade", foundByName.id)

        val foundByCyrillic = cityRepository.findByNameOrNameCyrillic("Тест Белград")
        assertNotNull(foundByCyrillic)
        assertEquals("test-belgrade", foundByCyrillic.id)

        val caseInsensitive = cityRepository.findByNameOrNameCyrillic("TEST BEOGRAD")
        assertNotNull(caseInsensitive)
        assertEquals("test-belgrade", caseInsensitive.id)

        val notFound = cityRepository.findByNameOrNameCyrillic("NonExistent")
        assertNull(notFound)

        val activeCities = cityRepository.findAllActive()
        assertTrue(activeCities.any { it.id == "test-belgrade" })

        val inactiveCity = City(
            id = "inactive-test",
            name = "Inactive Test",
            nameCyrillic = "Неактивни Тест",
            hasMup = false,
            active = false
        )
        cityRepository.save(inactiveCity)

        val allActiveCities = cityRepository.findAllActive()
        assertTrue(allActiveCities.none { it.id == "inactive-test" })
        assertTrue(allActiveCities.any { it.id == "test-belgrade" })
    }
}
