package rs.russian.portal.city.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import rs.russian.portal.city.dto.CityDto
import rs.russian.portal.city.service.CityService

@RestController
@RequestMapping("/api/cities")
class CityController(
    private val cityService: CityService
) {

    @GetMapping
    fun getAllCities(): List<CityDto> {
        return cityService.getAllActiveCities()
    }

    @GetMapping("/search")
    fun getCityByName(@RequestParam name: String): CityDto? {
        return cityService.getCityByName(name)
    }
}