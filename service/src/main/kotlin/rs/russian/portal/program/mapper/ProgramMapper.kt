package rs.russian.portal.program.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import rs.russian.generated.model.ProgramDto
import rs.russian.portal.program.domain.Program
import rs.russian.portal.program.domain.Project


@Mapper
abstract class ProgramMapper {

    @Mapping(target = "projectCodes", source = "projects")
    abstract fun toDto(program: Program?): ProgramDto

    protected fun map(projects: Set<Project>?): List<String> =
        projects?.map { it.code } ?: emptyList()
}
