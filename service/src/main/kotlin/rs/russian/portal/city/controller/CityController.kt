package rs.russian.portal.city.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.portal.city.service.CityService
import rs.russian.generated.api.CitiesApi
import rs.russian.generated.model.CityDto

@RestController
class CityController(
    private val cityService: CityService
) : CitiesApi {

    override fun getCities(): ResponseEntity<List<CityDto>> {
        return ResponseEntity.ok(cityService.getAllActiveCities())
    }

    override fun findCityByCode(code: String): ResponseEntity<CityDto> {
        return ResponseEntity.ok(cityService.findCityByCode(code))
    }

    override fun findCityByName(name: String): ResponseEntity<List<CityDto>> {
        return ResponseEntity.ok(cityService.findCitiesByName(name))
    }
}