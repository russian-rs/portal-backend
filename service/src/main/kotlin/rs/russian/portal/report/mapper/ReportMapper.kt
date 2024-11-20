package rs.russian.portal.report.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.ReportingPolicy.ERROR
import org.springframework.beans.factory.annotation.Autowired
import rs.russian.generated.model.ReportDto
import rs.russian.generated.model.TaskDto
import rs.russian.portal.file.mapper.FileInfoMapper
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.Task
import java.time.LocalDateTime
import java.util.*

@Mapper(
    componentModel = SPRING,
    unmappedTargetPolicy = ERROR,
    imports = [UUID::class, LocalDateTime::class, ArrayList::class],
    uses = [FileInfoMapper::class]
)
abstract class ReportMapper {

    @Autowired
    private lateinit var fileInfoMapper: FileInfoMapper

    @Mapping(target = "user", source = "account.username")
    abstract fun map(report: Report): ReportDto

    @Mapping(target = "customer", ignore = true) // fulfilled in the code
    @Mapping(target = "files", expression = "java(new ArrayList())") // fulfilled in the code
    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "version", expression = "java(LocalDateTime.now())")
    abstract fun map(taskDto: TaskDto, report: Report): Task

    @Mapping(target = "customer", source = "customer.username")
    abstract fun map(task: Task): TaskDto
}
