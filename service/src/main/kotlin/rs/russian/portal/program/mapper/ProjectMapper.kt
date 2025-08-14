package rs.russian.portal.program.mapper

import org.mapstruct.Mapper
import rs.russian.generated.model.ProjectDto
import rs.russian.portal.program.domain.Project

@Mapper
abstract class ProjectMapper {
    abstract fun toDto(project: Project): ProjectDto
}
