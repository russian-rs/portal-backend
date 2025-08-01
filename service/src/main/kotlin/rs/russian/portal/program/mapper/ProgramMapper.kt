package rs.russian.portal.program.mapper

import org.mapstruct.Mapper
import rs.russian.generated.model.ProgramDto
import rs.russian.portal.program.domain.Program

@Mapper
abstract class ProgramMapper {
    abstract fun toDto(program: Program): ProgramDto
}
