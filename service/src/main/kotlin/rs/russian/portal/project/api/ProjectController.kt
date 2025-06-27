package rs.russian.portal.project.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.ProjectsApi
import rs.russian.generated.model.ProjectDto
import rs.russian.portal.project.service.ProjectService

@RestController
class ProjectController(
    private val projectService: ProjectService
) : ProjectsApi {
    override fun getProjects(): ResponseEntity<List<ProjectDto>> {
        return ResponseEntity.ok(projectService.getProjects());
    }
}