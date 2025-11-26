package rs.russian.portal.program.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.ProgramsApi
import rs.russian.generated.api.ProjectsApi
import rs.russian.generated.model.ProgramDto
import rs.russian.generated.model.ProjectDto
import rs.russian.portal.program.service.ProgramService
import rs.russian.portal.program.service.ProjectService

@RestController
class ProgramController(
    private val programService: ProgramService,
    private val projectService: ProjectService
) : ProgramsApi, ProjectsApi {

    override fun getPrograms(): ResponseEntity<List<ProgramDto>> {
        return ResponseEntity.ok(programService.getPrograms())
    }

    override fun getProjects(): ResponseEntity<List<ProjectDto>> {
        return ResponseEntity.ok(projectService.getProjects())
    }
}