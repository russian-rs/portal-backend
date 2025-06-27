package rs.russian.portal.project.mapper

import org.mapstruct.Mapper
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.ReportingPolicy
import rs.russian.generated.model.ProjectDto
import rs.russian.portal.project.domain.Project

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
abstract class ProjectMapper {
    abstract fun toDto(project: Project): ProjectDto
}