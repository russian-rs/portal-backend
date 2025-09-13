package rs.russian.portal.city.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.city.mapper.CityMapper
import rs.russian.portal.city.repository.CityRepository
import rs.russian.portal.shared.utils.CacheService.Companion.CITIES_CACHE_NAME
import rs.russian.generated.model.CityDto

@Service
@Transactional(readOnly = true)
class CityService(
    private val cityRepository: CityRepository,
    private val cityMapper: CityMapper
) {

    @Cacheable(cacheNames = [CITIES_CACHE_NAME])
    fun getAllActiveCities(): List<CityDto> {
        val cities = cityRepository.findAllActive()
        return cityMapper.toDtoList(cities)
    }

    fun findCitiesByName(searchTerm: String): List<CityDto> {
        val cities = cityRepository.findByNameOrNameCyrillic(searchTerm)
        return cityMapper.toDtoList(cities)
    }

    fun findCityByCode(code: String): CityDto {
        val city = cityRepository.findById(code)
            .orElseThrow { EntityNotFoundException("City with code '$code' not found") }
        return cityMapper.toDto(city)
    }
}
