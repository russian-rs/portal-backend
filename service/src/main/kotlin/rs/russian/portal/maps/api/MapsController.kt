package rs.russian.portal.maps.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.MapsApi
import rs.russian.generated.model.PlaygroundDto
import rs.russian.generated.model.VolunteerMapDto
import rs.russian.portal.maps.service.MapsService

@RestController
class MapsController(
    private val mapsService: MapsService
) : MapsApi {

    override fun getVolunteersMap(): ResponseEntity<List<VolunteerMapDto>> {
        return ResponseEntity.ok(mapsService.getVolunteersMap())
    }

    override fun getPlaygroundsMap(): ResponseEntity<List<PlaygroundDto>> {
        return ResponseEntity.ok(mapsService.getPlaygroundsMap())
    }
}
