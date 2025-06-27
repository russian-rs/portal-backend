package rs.russian.portal.project.service

import org.springframework.stereotype.Service
import rs.russian.generated.model.ProjectDto
import rs.russian.portal.project.mapper.ProjectMapper
import rs.russian.portal.project.repository.ProjectRepository

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectMapper: ProjectMapper
) {

    fun getProjects(): List<ProjectDto>? {
        return projectRepository.findAll().map { projectMapper.toDto(it) }
    }
}