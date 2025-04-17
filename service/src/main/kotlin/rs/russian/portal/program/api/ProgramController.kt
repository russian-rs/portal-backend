package rs.russian.portal.program.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.ProgramsApi
import rs.russian.generated.model.ProgramDto
import rs.russian.portal.program.service.ProgramService

@RestController
class ProgramController(private val programService: ProgramService) : ProgramsApi {
    override fun getPrograms(): ResponseEntity<List<ProgramDto>> {
        return ResponseEntity.ok(programService.getPrograms())
    }
}