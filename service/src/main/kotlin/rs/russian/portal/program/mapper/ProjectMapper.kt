package rs.russian.portal.program.mapper

import org.mapstruct.Mapper
import org.mapstruct.MappingConstants
import org.mapstruct.ReportingPolicy
import rs.russian.generated.model.ProjectDto
import rs.russian.portal.program.domain.Project

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
abstract class ProjectMapper {
    abstract fun toDto(project: Project): ProjectDto
}