package rs.russian.portal.program.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.OfficialGroupApi
import rs.russian.generated.api.ProgramsApi
import rs.russian.generated.api.ProjectsApi
import rs.russian.generated.model.OfficialGroupDto
import rs.russian.generated.model.ProgramDto
import rs.russian.generated.model.ProjectDto
import rs.russian.portal.program.service.OfficialGroupService
import rs.russian.portal.program.service.ProgramService
import rs.russian.portal.program.service.ProjectService

@RestController
class ProgramController(
    private val programService: ProgramService,
    private val projectService: ProjectService,
    private val officialGroupService: OfficialGroupService
) : ProgramsApi, ProjectsApi, OfficialGroupApi {

    override fun getPrograms(): ResponseEntity<List<ProgramDto>> {
        return ResponseEntity.ok(programService.getPrograms())
    }

    override fun getProjects(): ResponseEntity<List<ProjectDto>> {
        return ResponseEntity.ok(projectService.getProjects())
    }

    override fun getOfficialGroup(): ResponseEntity<List<OfficialGroupDto>> {
        return ResponseEntity.ok(officialGroupService.getOfficialGroup())
    }
}