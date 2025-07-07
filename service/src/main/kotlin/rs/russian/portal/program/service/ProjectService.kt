package rs.russian.portal.program.service

import org.springframework.stereotype.Service
import rs.russian.generated.model.ProjectDto
import rs.russian.portal.program.mapper.ProjectMapper
import rs.russian.portal.program.repository.ProjectRepository

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectMapper: ProjectMapper
) {

    fun getProjects(): List<ProjectDto>? {
        return projectRepository.findAll().map { projectMapper.toDto(it) }
    }
}