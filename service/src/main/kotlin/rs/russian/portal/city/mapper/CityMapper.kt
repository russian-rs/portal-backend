package rs.russian.portal.city.mapper

import org.mapstruct.Mapper
import rs.russian.portal.city.domain.City
import rs.russian.generated.model.CityDto

@Mapper
abstract class CityMapper {

    abstract fun toDto(city: City): CityDto

    abstract fun toDtoList(cities: List<City>): List<CityDto>
}