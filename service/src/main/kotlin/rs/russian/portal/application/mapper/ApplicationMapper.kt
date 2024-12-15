package rs.russian.portal.application.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.MappingTarget
import org.mapstruct.ReportingPolicy.ERROR
import rs.russian.generated.model.ApplicationDto
import rs.russian.generated.model.ApplicationStatusDto
import rs.russian.portal.application.domain.Application
import rs.russian.portal.application.domain.ApplicationStatus
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ERROR, imports = [UUID::class])
abstract class ApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "refuseReason", ignore = true)
    abstract fun map(applicationDto: ApplicationDto, @MappingTarget application: Application)

    abstract fun map(application: Application): ApplicationDto

    @Mapping(target = "progress", source = "status")
    @Mapping(target = "terminated", source = "status")
    @Mapping(target = "lastUpdate", source = "version")
    abstract fun mapStatus(application: Application): ApplicationStatusDto

    fun mapProgress(status: ApplicationStatus): Int = status.progress

    fun mapTerminated(status: ApplicationStatus): Boolean = status.terminated

    fun map(value: LocalDateTime): OffsetDateTime {
        return OffsetDateTime.of(value, ZoneOffset.UTC)
    }
}
