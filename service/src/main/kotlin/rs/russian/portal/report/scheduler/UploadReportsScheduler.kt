package rs.russian.portal.report.scheduler

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import rs.russian.portal.file.service.S3Service
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.Task
import rs.russian.portal.report.service.ReportService
import rs.russian.portal.shared.enums.ReportStatus
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.service.AccountService
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

@Component
class UploadReportsScheduler(
    private val s3Service: S3Service,
    private val objectMapper: ObjectMapper,
    private val reportService: ReportService,
    private val accountService: AccountService
) {

    @Scheduled(cron = "-")
    fun upload(): Unit = runBlocking {
        val text = s3Service.file("/reports/reports.json")
        val customer = accountService.findAccountByLogin("ruskadijaspora")!!
        val reports = objectMapper.readValue(text, object : TypeReference<List<Map<String, String>>>() {})
        reports.forEach { report ->
            try {
                createReport(report, customer)
            } catch (e: Exception) {
                log.error("Failed to create report ${report["Эл.почта"]} - ${report["Неделя"]}", e)
            }
        }
    }

    private fun createReport(data: Map<String, String>, customer: Account) {
        val account = accountService.findAccountByEmail(data["Эл.почта"]) ?: return
        if (data["Тип отчета"] != "Факт") {
            return
        }
        val existReport = reportService.findByAccountAndHash(account, data.hashCode())
        if (existReport != null) {
            return
        }
        val report = Report(account = account, status = ReportStatus.ACCEPTED, hash = data.hashCode())
        if (data["Кол-во задач"].isNullOrBlank()) {
            return
        }
        val taskCount = data["Кол-во задач"]!!.toDouble().toInt()
        if (taskCount < 1) {
            return
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val tasks = mutableListOf<Task>()
        val week = data["Неделя"]!!.toDouble().toLong()
        val defaultDate = LocalDate.ofYearDay(2024, 1)
            .with(WeekFields.ISO.weekOfYear(), week)
            .with(WeekFields.ISO.dayOfWeek(), 1)
        for (i in taskCount downTo 1) {
            var date = defaultDate
            try {
                date = LocalDate.parse(data["Дата $i"]!!.substringBefore('T'), formatter)
            } catch (e: Exception) {
            }
            val name = data["Задача $i"] ?: "Задача $i"
            var timeSpent = 10
            try {
                timeSpent = data["Кол-во часов $i"]!!.toDouble().toInt() * 60
            } catch (e: Exception) {
            }
            val description = data["Описание $i"] ?: ""
            val task = Task(
                date = date,
                name = name,
                description = description,
                timeSpent = timeSpent,
                report = report,
                customer = customer
            )
            tasks.add(task)
        }
        if (tasks.size < 1) {
            return
        }
        report.createTime = OffsetDateTime.of(LocalDateTime.of(tasks[0].date, LocalTime.now()), ZoneOffset.UTC)
        report.tasks = tasks
        reportService.save(report)
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
