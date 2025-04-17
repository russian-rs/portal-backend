package rs.russian.portal.program.mapper

import org.mapstruct.Mapper
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.ReportingPolicy
import rs.russian.generated.model.ProgramDto
import rs.russian.portal.program.domain.Program

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
abstract class ProgramMapper {
    abstract fun toDto(program: Program): ProgramDto
}