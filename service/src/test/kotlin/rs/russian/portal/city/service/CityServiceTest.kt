package rs.russian.portal.city.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rs.russian.portal.city.domain.City
import rs.russian.portal.city.dto.CityDto
import rs.russian.portal.city.mapper.CityMapper
import rs.russian.portal.city.repository.CityRepository
import java.time.LocalDateTime

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
                id = "belgrade",
                name = "Beograd",
                nameCyrillic = "Белград",
                hasMup = true,
                active = true,
                version = LocalDateTime.now()
            ),
            City(
                id = "novi-sad",
                name = "Novi Sad",
                nameCyrillic = "Нови Сад",
                hasMup = true,
                active = true,
                version = LocalDateTime.now()
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
    fun `findCityByName should return mapped city when found using Latin name`() {
        val city = City(
            id = "nis",
            name = "Niš",
            nameCyrillic = "Ниш",
            hasMup = true,
            active = true,
            version = LocalDateTime.now()
        )
        
        val cityDto = CityDto(
            code = "nis",
            name = "Niš",
            nameCyrillic = "Ниш",
            hasMup = true
        )

        every { cityRepository.findByNameOrNameCyrillic("Niš") } returns city
        every { cityMapper.toDto(city) } returns cityDto

        val result = cityService.findCityByName("Niš")

        assertNotNull(result)
        assertEquals("nis", result?.code)
        assertEquals("Niš", result?.name)
        assertEquals("Ниш", result?.nameCyrillic)

        verify(exactly = 1) { cityRepository.findByNameOrNameCyrillic("Niš") }
        verify(exactly = 1) { cityMapper.toDto(city) }
    }

    @Test
    fun `findCityByName should return mapped city when found using Cyrillic name`() {
        val city = City(
            id = "belgrade",
            name = "Beograd",
            nameCyrillic = "Белград",
            hasMup = true,
            active = true,
            version = LocalDateTime.now()
        )
        
        val cityDto = CityDto(
            code = "belgrade",
            name = "Beograd",
            nameCyrillic = "Белград",
            hasMup = true
        )

        every { cityRepository.findByNameOrNameCyrillic("Белград") } returns city
        every { cityMapper.toDto(city) } returns cityDto

        val result = cityService.findCityByName("Белград")

        assertNotNull(result)
        assertEquals("belgrade", result?.code)
        assertEquals("Beograd", result?.name)
        assertEquals("Белград", result?.nameCyrillic)

        verify(exactly = 1) { cityRepository.findByNameOrNameCyrillic("Белград") }
        verify(exactly = 1) { cityMapper.toDto(city) }
    }

    @Test
    fun `findCityByName should return null when city not found`() {
        every { cityRepository.findByNameOrNameCyrillic("NonExistent") } returns null

        val result = cityService.findCityByName("NonExistent")

        assertNull(result)
        verify(exactly = 1) { cityRepository.findByNameOrNameCyrillic("NonExistent") }
        verify(exactly = 0) { cityMapper.toDto(any()) }
    }

    @Test
    fun `findCityByName should handle case insensitive search`() {
        val city = City(
            id = "cacak",
            name = "Čačak",
            nameCyrillic = "Чачак",
            hasMup = true,
            active = true,
            version = LocalDateTime.now()
        )
        
        val cityDto = CityDto(
            code = "cacak",
            name = "Čačak",
            nameCyrillic = "Чачак",
            hasMup = true
        )

        every { cityRepository.findByNameOrNameCyrillic("ČAČAK") } returns city
        every { cityMapper.toDto(city) } returns cityDto

        val result = cityService.findCityByName("ČAČAK")

        assertNotNull(result)
        assertEquals("cacak", result?.code)
        assertEquals("Čačak", result?.name)

        verify(exactly = 1) { cityRepository.findByNameOrNameCyrillic("ČAČAK") }
    }

    @Test
    fun `findCityByCode should return mapped city when found`() {
        val city = City(
            id = "belgrade",
            name = "Beograd",
            nameCyrillic = "Белград",
            hasMup = true,
            active = true,
            version = LocalDateTime.now()
        )
        
        val cityDto = CityDto(
            code = "belgrade",
            name = "Beograd",
            nameCyrillic = "Белград",
            hasMup = true
        )

        every { cityRepository.findById("belgrade") } returns java.util.Optional.of(city)
        every { cityMapper.toDto(city) } returns cityDto

        val result = cityService.findCityByCode("belgrade")

        assertNotNull(result)
        assertEquals("belgrade", result?.code)
        assertEquals("Beograd", result?.name)
        assertEquals("Белград", result?.nameCyrillic)

        verify(exactly = 1) { cityRepository.findById("belgrade") }
        verify(exactly = 1) { cityMapper.toDto(city) }
    }

    @Test
    fun `findCityByCode should return null when city not found`() {
        every { cityRepository.findById("non-existent") } returns java.util.Optional.empty()

        val result = cityService.findCityByCode("non-existent")

        assertNull(result)
        verify(exactly = 1) { cityRepository.findById("non-existent") }
        verify(exactly = 0) { cityMapper.toDto(any()) }
    }
}