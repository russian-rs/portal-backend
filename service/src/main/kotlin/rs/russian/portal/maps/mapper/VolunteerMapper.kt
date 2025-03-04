package rs.russian.portal.maps.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.ReportingPolicy.ERROR
import rs.russian.generated.model.VolunteerMapDto
import rs.russian.portal.user.domain.Account

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ERROR)
interface VolunteerMapper {

    @Mapping(target = "telegram", source = "info.telegram")
    @Mapping(target = "city", source = "info.city")
    @Mapping(target = "address", source = "info.address")
    @Mapping(target = "latitude", source = "info.latitude")
    @Mapping(target = "longitude", source = "info.longitude")
    fun map(account: Account): VolunteerMapDto
}
