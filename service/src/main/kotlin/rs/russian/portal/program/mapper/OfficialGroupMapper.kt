package rs.russian.portal.program.mapper

import org.mapstruct.Mapper
import rs.russian.generated.model.OfficialGroupDto
import rs.russian.portal.program.domain.OfficialGroup


@Mapper
abstract class OfficialGroupMapper {
    abstract fun toDto(officalGroup: OfficialGroup?): OfficialGroupDto
}
