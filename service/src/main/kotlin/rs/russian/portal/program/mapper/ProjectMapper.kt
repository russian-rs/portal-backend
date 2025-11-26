package rs.russian.portal.program.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import rs.russian.generated.model.ProjectDto
import rs.russian.portal.program.domain.Project


@Mapper
abstract class ProjectMapper {
    @Mapping(target = "programCode", source = "program.code")
    abstract fun toDto(project: Project?): ProjectDto
}
