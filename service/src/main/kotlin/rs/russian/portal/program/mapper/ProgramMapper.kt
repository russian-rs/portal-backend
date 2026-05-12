package rs.russian.portal.program.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import rs.russian.generated.model.ProgramDto
import rs.russian.portal.program.domain.OfficialGroup
import rs.russian.portal.program.domain.Program
import rs.russian.portal.program.domain.Project


@Mapper
abstract class ProgramMapper {

    @Mapping(target = "projectCodes", source = "projects", qualifiedByName = ["projectCodes"])
    @Mapping(target = "officialGroup", source = "officialGroup", qualifiedByName = ["officialGroupCode"])
    abstract fun toDto(program: Program?): ProgramDto

    @Named("projectCodes")
    protected fun mapProjects(projects: Set<Project>?): List<String> =
        projects?.map { it.code } ?: emptyList()

    @Named("officialGroupCode")
    protected fun mapOfficialGroup(officialGroup: OfficialGroup?): String? = officialGroup?.code
}
