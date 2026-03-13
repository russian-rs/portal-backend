package rs.russian.portal.report.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.beans.factory.annotation.Autowired
import rs.russian.generated.model.CreateTaskRequest
import rs.russian.generated.model.ReportDto
import rs.russian.generated.model.ReportView
import rs.russian.generated.model.TaskDto
import rs.russian.portal.file.mapper.FileInfoMapper
import rs.russian.portal.note.mapper.NoteMapper
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.Task
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.*

@Mapper(
    imports = [UUID::class, LocalDateTime::class, HashSet::class],
    uses = [FileInfoMapper::class, NoteMapper::class]
)
abstract class ReportMapper {

    @Autowired
    private lateinit var fileInfoMapper: FileInfoMapper

    @Autowired
    private lateinit var noteMapper: NoteMapper

    @Mapping(target = "program", source = "program.code")
    @Mapping(target = "project", source = "project.code")
    @Mapping(target = "week", source = "report", qualifiedByName = ["reportWeek"])
    @Mapping(target = "user", source = "account.username")
    @Mapping(target = "isAuto", constant = "false")
    abstract fun map(report: Report): ReportDto

    abstract fun map(reports: List<Report>): MutableList<ReportDto>

    @Mapping(target = "customer", ignore = true) // fulfilled in the code
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "files", expression = "java(new HashSet())") // fulfilled in the code
    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    abstract fun map(taskDto: TaskDto, report: Report): Task

    @Mapping(target = "customer", source = "customer.username")
    abstract fun map(task: Task): TaskDto

    @Mapping(target = "customer", ignore = true) // fulfilled in the code
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "files", expression = "java(new HashSet())") // fulfilled in the code
    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    abstract fun map(createTaskRequest: CreateTaskRequest, report: Report): Task

    @Mapping(target = "week", source = "report", qualifiedByName = ["reportWeek"])
    @Mapping(target = "user", source = "account.username")
    abstract fun mapView(report: Report): ReportView

    @Named("reportWeek")
    fun week(report: Report) =
        report.tasks.maxOf { it.date }.get(WeekFields.of(Locale.of("sr", "RS")).weekOfWeekBasedYear())
}
