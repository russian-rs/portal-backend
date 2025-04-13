package rs.russian.portal.user.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.ReportingPolicy
import rs.russian.generated.model.ProgramDto
import rs.russian.portal.user.domain.Program

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
abstract class ProgramMapper {
    @Mapping(target = "copy", ignore = true)
    abstract fun toDto(program: Program): ProgramDto
}