package rs.russian.portal.report.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.ReportingPolicy.ERROR
import rs.russian.generated.model.ReportDto
import rs.russian.generated.model.TaskDto
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.Task
import java.time.LocalDateTime
import java.util.*

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ERROR, imports = [UUID::class, LocalDateTime::class])
interface ReportMapper {

    @Mapping(target = "user", source = "account.username")
    fun map(report: Report): ReportDto

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "version", expression = "java(LocalDateTime.now())")
    fun map(taskDto: TaskDto, report: Report): Task

    fun map(task: Task): TaskDto
}
