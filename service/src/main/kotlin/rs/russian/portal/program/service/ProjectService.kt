package rs.russian.portal.program.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.ProjectDto
import rs.russian.portal.program.mapper.ProjectMapper
import rs.russian.portal.program.repository.ProjectRepository
import rs.russian.portal.shared.utils.CacheService.Companion.PROJECT_DICT_CACHE_NAME

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectMapper: ProjectMapper
) {

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = [PROJECT_DICT_CACHE_NAME], key = "'all'")
    fun getProjects(): List<ProjectDto> =
        projectRepository.findAll().map(projectMapper::toDto)

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = [PROJECT_DICT_CACHE_NAME], key = "'byProgram:' + #code")
    fun getProjectsByProgram(code: String): List<ProjectDto> =
        projectRepository.findAll()
            .asSequence()
            .filter { project -> project.programs.any { it.code == code } }
            .map(projectMapper::toDto)
            .toList()
}