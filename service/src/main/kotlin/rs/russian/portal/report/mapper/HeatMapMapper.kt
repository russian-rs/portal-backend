package rs.russian.portal.report.mapper

import org.mapstruct.Mapper
import rs.russian.generated.model.HeatMapItem
import rs.russian.portal.report.repository.projections.VolunteerWeekProjection

@Mapper
interface HeatMapMapper {

    fun map(projection: VolunteerWeekProjection): HeatMapItem

    fun map(projections: List<VolunteerWeekProjection>): List<HeatMapItem>
}
