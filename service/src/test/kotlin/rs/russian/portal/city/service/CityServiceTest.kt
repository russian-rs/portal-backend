package rs.russian.portal.city.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rs.russian.portal.city.domain.City
import rs.russian.portal.city.mapper.CityMapper
import rs.russian.portal.city.repository.CityRepository
import rs.russian.generated.model.CityDto
import java.util.Optional

class CityServiceTest {

    private lateinit var cityRepository: CityRepository
    private lateinit var cityMapper: CityMapper
    private lateinit var cityService: CityService

    @BeforeEach
    fun setup() {
        cityRepository = mockk()
        cityMapper = mockk()
        cityService = CityService(cityRepository, cityMapper)
    }

    @Test
    fun `getAllActiveCities should return mapped list of active cities`() {
        val cities = listOf(
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
            )
        )
        
        val cityDtos = listOf(
            CityDto(
                code = "belgrade",
                name = "Beograd",
                nameCyrillic = "Белград",
                hasMup = true
            ),
            CityDto(
                code = "novi-sad",
                name = "Novi Sad",
                nameCyrillic = "Нови Сад",
                hasMup = true
            )
        )

        every { cityRepository.findAllActive() } returns cities
        every { cityMapper.toDtoList(cities) } returns cityDtos

        val result = cityService.getAllActiveCities()

        assertEquals(2, result.size)
        assertEquals("belgrade", result[0].code)
        assertEquals("Beograd", result[0].name)
        assertEquals("Белград", result[0].nameCyrillic)
        assertEquals("novi-sad", result[1].code)
        assertEquals("Novi Sad", result[1].name)
        assertEquals("Нови Сад", result[1].nameCyrillic)

        verify(exactly = 1) { cityRepository.findAllActive() }
        verify(exactly = 1) { cityMapper.toDtoList(cities) }
    }


    @Test
    fun `getAllActiveCities should return empty list when no active cities`() {
        every { cityRepository.findAllActive() } returns emptyList()
        every { cityMapper.toDtoList(emptyList()) } returns emptyList()

        val result = cityService.getAllActiveCities()

        assertTrue(result.isEmpty())
        verify(exactly = 1) { cityRepository.findAllActive() }
    }

    @Test
    fun `findCitiesByName should return mapped cities when found`() {
        val cities = listOf(
            City(
                code = "nis",
                name = "Niš",
                nameCyrillic = "Ниш",
                hasMup = true,
                active = true
            )
        )
        
        val cityDtos = listOf(
            CityDto(
                code = "nis",
                name = "Niš",
                nameCyrillic = "Ниш",
                hasMup = true
            )
        )

        every { cityRepository.findByNameOrNameCyrillic("Niš") } returns cities
        every { cityMapper.toDtoList(cities) } returns cityDtos

        val result = cityService.findCitiesByName("Niš")

        assertEquals(1, result.size)
        assertEquals("nis", result[0].code)
        assertEquals("Niš", result[0].name)
        assertEquals("Ниш", result[0].nameCyrillic)

        verify(exactly = 1) { cityRepository.findByNameOrNameCyrillic("Niš") }
        verify(exactly = 1) { cityMapper.toDtoList(cities) }
    }

    @Test
    fun `findCitiesByName should return empty list when no cities found`() {
        every { cityRepository.findByNameOrNameCyrillic("NonExistent") } returns emptyList()
        every { cityMapper.toDtoList(emptyList()) } returns emptyList()

        val result = cityService.findCitiesByName("NonExistent")

        assertTrue(result.isEmpty())
        verify(exactly = 1) { cityRepository.findByNameOrNameCyrillic("NonExistent") }
    }

    @Test
    fun `findCityByCode should return mapped city when found`() {
        val city = City(
            code = "belgrade",
            name = "Beograd",
            nameCyrillic = "Белград",
            hasMup = true,
            active = true
        )
        
        val cityDto = CityDto(
            code = "belgrade",
            name = "Beograd",
            nameCyrillic = "Белград",
            hasMup = true
        )

        every { cityRepository.findById("belgrade") } returns Optional.of(city)
        every { cityMapper.toDto(city) } returns cityDto

        val result = cityService.findCityByCode("belgrade")

        assertEquals("belgrade", result.code)
        assertEquals("Beograd", result.name)
        assertEquals("Белград", result.nameCyrillic)
        assertTrue(result.hasMup)

        verify(exactly = 1) { cityRepository.findById("belgrade") }
        verify(exactly = 1) { cityMapper.toDto(city) }
    }

    @Test
    fun `findCityByCode should throw EntityNotFoundException when city not found`() {
        every { cityRepository.findById("non-existent") } returns Optional.empty()

        val exception = assertThrows(EntityNotFoundException::class.java) {
            cityService.findCityByCode("non-existent")
        }

        assertEquals("City with code 'non-existent' not found", exception.message)
        verify(exactly = 1) { cityRepository.findById("non-existent") }
    }
}