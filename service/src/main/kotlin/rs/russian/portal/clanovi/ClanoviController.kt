package rs.russian.portal.clanovi

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.model.ApplicationDto
import java.util.UUID

/**
 * Read-only feed for the Clanovi office app. Auth is the permanent [ClanoviApiKeyFilter] header,
 * not a browser session. Existing `/application` website routes are unchanged.
 */
@RestController
@RequestMapping("/clanovi")
class ClanoviController(
    private val clanoviApplicationService: ClanoviApplicationService,
) {

    @GetMapping("/applications")
    fun listApplications(
        @RequestParam(required = false) since: String?,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) open: Boolean?,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ClanoviApplicationListResponse> =
        ResponseEntity.ok(clanoviApplicationService.listChanged(since, limit, open, page))

    @GetMapping("/application/{id}")
    fun getApplication(@PathVariable id: UUID): ResponseEntity<ApplicationDto> =
        ResponseEntity.ok(clanoviApplicationService.get(id))
}
