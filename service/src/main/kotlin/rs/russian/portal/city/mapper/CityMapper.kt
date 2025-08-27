package rs.russian.portal.city.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import rs.russian.portal.city.domain.City
import rs.russian.portal.city.dto.CityDto

@Mapper
abstract class CityMapper {

    @Mapping(source = "id", target = "code")
    abstract fun toDto(city: City): CityDto

    abstract fun toDtoList(cities: List<City>): List<CityDto>
}