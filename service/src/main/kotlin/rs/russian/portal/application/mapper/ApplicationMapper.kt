package rs.russian.portal.application.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.MappingTarget
import org.mapstruct.Named
import org.mapstruct.ReportingPolicy.ERROR
import rs.russian.generated.model.ApplicationDto
import rs.russian.generated.model.ApplicationStatusDto
import rs.russian.generated.model.ContractDto
import rs.russian.generated.model.ContractTypeEnum
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
    @Mapping(target = "contractFrom", source = "contract.startDate")
    @Mapping(target = "contractUntil", source = "contract.endDate")
    abstract fun map(applicationDto: ApplicationDto, @MappingTarget application: Application)

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "contractFrom", source = "contract.startDate")
    @Mapping(target = "contractUntil", source = "contract.endDate")
    abstract fun update(applicationDto: ApplicationDto, @MappingTarget application: Application)

    @Mapping(target = "contract", source = "application", qualifiedByName = ["contract"])
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

    @Named("contract")
    fun mapContract(application: Application): ContractDto? {
        if (application.contractFrom == null || application.contractUntil == null) {
            return null
        }
        val contractType =
            if (application.residenceRequired == true) ContractTypeEnum.REGULAR else ContractTypeEnum.ASSOCIATED
        return ContractDto(
            id = UUID.randomUUID(),
            startDate = application.contractFrom!!,
            endDate = application.contractUntil!!,
            type = contractType
        )
    }
}
