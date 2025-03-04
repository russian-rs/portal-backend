package rs.russian.portal.maps.mapper

import org.mapstruct.Mapper
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.ReportingPolicy.ERROR
import rs.russian.generated.model.PlaygroundDto
import rs.russian.portal.maps.domain.Playground

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ERROR)
interface PlaygroundMapper {

    fun map(playground: Playground): PlaygroundDto
}
