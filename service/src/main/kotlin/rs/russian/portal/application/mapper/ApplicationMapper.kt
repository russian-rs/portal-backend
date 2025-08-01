package rs.russian.portal.application.mapper

import org.mapstruct.*
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.ReportingPolicy.ERROR
import rs.russian.generated.model.ApplicationDto
import rs.russian.generated.model.ApplicationStatusDto
import rs.russian.generated.model.ContractDto
import rs.russian.portal.application.domain.Application
import rs.russian.portal.application.domain.ApplicationStatus
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.UserInfo
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Mapper(
    componentModel = SPRING,
    imports = [UUID::class],
    unmappedTargetPolicy = ERROR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
abstract class ApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "refuseReason", ignore = true)
    @Mapping(target = "contractFrom", source = "contract.startDate")
    @Mapping(target = "contractUntil", source = "contract.endDate")
    @Mapping(target = "contractType", source = "contract.type")
    abstract fun toEntity(applicationDto: ApplicationDto, @MappingTarget application: Application)

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "contractFrom", source = "contract.startDate")
    @Mapping(target = "contractUntil", source = "contract.endDate")
    @Mapping(target = "contractType", source = "contract.type")
    abstract fun update(applicationDto: ApplicationDto, @MappingTarget application: Application)

    @Mapping(target = "contract", source = "application", qualifiedByName = ["contract"])
    abstract fun toDto(application: Application): ApplicationDto

    @Mapping(target = "progress", source = "status")
    @Mapping(target = "terminated", source = "status")
    @Mapping(target = "lastUpdate", source = "version")
    abstract fun mapStatus(application: Application): ApplicationStatusDto

    @Mapping(target = "account", source = "account")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "program", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    abstract fun mapToInfo(application: Application, account: Account): UserInfo

    fun mapProgress(status: ApplicationStatus): Int = status.progress

    fun mapTerminated(status: ApplicationStatus): Boolean = status.terminated

    fun map(value: LocalDateTime): OffsetDateTime {
        return OffsetDateTime.of(value, ZoneOffset.UTC)
    }

    @Named("contract")
    fun mapContract(application: Application): ContractDto? {
        if (application.contractFrom == null || application.contractUntil == null || application.contractType == null) {
            return null
        }
        return ContractDto(
            id = UUID.randomUUID(),
            startDate = application.contractFrom!!,
            endDate = application.contractUntil!!,
            type = application.contractType!!
        )
    }
}
