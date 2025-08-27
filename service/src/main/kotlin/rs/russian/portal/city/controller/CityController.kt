package rs.russian.portal.city.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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

    @GetMapping("/{code}")
    fun findCityByCode(@PathVariable code: String): ResponseEntity<CityDto> {
        val city = cityService.findCityByCode(code)
        return if (city != null) {
            ResponseEntity.ok(city)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/search")
    fun findCityByName(@RequestParam name: String): ResponseEntity<CityDto> {
        val city = cityService.findCityByName(name)
        return if (city != null) {
            ResponseEntity.ok(city)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}