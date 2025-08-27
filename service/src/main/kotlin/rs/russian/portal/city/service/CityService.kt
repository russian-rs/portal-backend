package rs.russian.portal.city.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.city.dto.CityDto
import rs.russian.portal.city.mapper.CityMapper
import rs.russian.portal.city.repository.CityRepository
import rs.russian.portal.shared.utils.CacheService.Companion.CITIES_CACHE_NAME

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

    fun getCityByName(name: String): CityDto? {
        val city = cityRepository.findByNameIgnoreCase(name)
        return city?.let { cityMapper.toDto(it) }
    }
}
