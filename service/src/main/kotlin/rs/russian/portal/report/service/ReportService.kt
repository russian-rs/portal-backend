package rs.russian.portal.report.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import rs.russian.generated.model.NoteDto
import rs.russian.generated.model.ReportDto
import rs.russian.generated.model.ReportFilter
import rs.russian.generated.model.StatisticData
import rs.russian.generated.model.Statistics
import rs.russian.portal.file.service.FileService
import rs.russian.portal.note.domain.Note
import rs.russian.portal.note.domain.enums.EntityType
import rs.russian.portal.note.service.NoteService
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.Task
import rs.russian.portal.report.domain.enums.ReportStatus
import rs.russian.portal.report.domain.specification.from
import rs.russian.portal.report.mapper.ReportMapper
import rs.russian.portal.report.repository.ReportRepository
import rs.russian.portal.shared.exception.NotAuthorizedException
import rs.russian.portal.shared.security.currentUserLogin
import rs.russian.portal.user.service.AccountService
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class ReportService(
    private val accountService: AccountService,
    private val fileService: FileService,
    private val reportMapper: ReportMapper,
    private val noteService: NoteService,
    private val reportRepository: ReportRepository
) {

    @Transactional(readOnly = true)
    fun getReport(reportId: UUID): Report {
        return reportRepository.findById(reportId).orElseThrow()
    }

    @Transactional
    fun createReport(reportDto: ReportDto): Report {
        val report = Report(account = accountService.getCurrentAccount(), status = ReportStatus.CREATED)
        val tasks = reportDto.tasks.map { taskDto ->
            reportMapper.map(taskDto, report).also { task ->
                task.customer = accountService.findAccountByLogin(taskDto.customer)
                task.files = fileService.findAllByIds(taskDto.files?.map { it.id }?.toSet())
            }
        }
        return reportRepository.save(report.also { it.tasks = tasks.toMutableSet() })
    }

    @Transactional
    fun deleteReport(reportId: UUID) {
        val report = getReport(reportId)
        reportRepository.save(report.also { it.deleted = true })
    }

    @Transactional
    fun updateReport(reportDto: ReportDto): Report {
        val report = getReport(reportDto.id)
        val tasks = reportDto.tasks.map { taskDto ->
            reportMapper.map(taskDto, report).also { task ->
                task.customer = accountService.findAccountByLogin(taskDto.customer)
                task.files = fileService.findAllByIds(taskDto.files?.map { it.id }?.toSet())
            }
        }
        report.status = ReportStatus.CREATED
        report.tasks.clear()
        return reportRepository.save(report.also { it.tasks.addAll(tasks) })
    }

    @Transactional(readOnly = true)
    fun getReports(reportFilter: ReportFilter, pageable: Pageable): Page<Report> {
        return reportRepository.findAll(from(reportFilter), pageable)
    }

    @Transactional
    fun save(report: Report): Report {
        return reportRepository.save(report)
    }

    @Transactional
    fun addNote(reportId: UUID, noteDto: NoteDto): Note {
        val report = getReport(reportId)
        val currentAccount = accountService.getAccountByLogin(currentUserLogin() ?: throw NotAuthorizedException())
        val note = noteService.save(
            Note(
                createdBy = currentAccount,
                entityId = reportId,
                entityType = EntityType.REPORT,
                text = noteDto.text
            )
        )
        report.notes.add(note)
        return note
    }

    @Transactional
    fun changeStatus(reportId: UUID, status: ReportStatus, noteText: String? = null) {
        val report = getReport(reportId)
        if (!noteText.isNullOrEmpty()) {
            val currentAccount = accountService.getAccountByLogin(currentUserLogin() ?: throw NotAuthorizedException())
            val note = noteService.save(
                Note(
                    createdBy = currentAccount,
                    entityId = reportId,
                    entityType = EntityType.REPORT,
                    text = noteText
                )
            )
            report.notes.add(note)
        }
        report.status = status
    }

    @Transactional(readOnly = true)
    fun getStatistics(year: Int): Statistics {

        val start = OffsetDateTime.of(
            year, 1, 1, 0, 0, 0,
            0, ZoneOffset.UTC
        )
        val end = start.plusYears(1).minusNanos(1)
        val reports = reportRepository.findByCreateTimeBetween(start, end)

        return collectStatistics(reports)
    }

    private fun collectStatistics(reports: List<Report>): Statistics {
        val byGroup: Map<String, List<Report>> = reports.groupBy {
            CODE_TO_GROUP[it.account.info?.project?.code] ?: "other"
        }

        val statsMap: Map<String, StatisticData> = byGroup.mapValues { (_, reps) ->
            val usersCount = reps.flatMap { listOf(it.account.username) }.toSet().size
            val totalMinutes = reps.sumOf { it.tasks.sumOf(Task::timeSpent) }
            StatisticData(count = usersCount, totalTimeSpent = totalMinutes / 60.0)
        }
        val stats = Statistics(
            socialSecurity = statsMap["socialSecurity"] ?: StatisticData(0, 0.0),
            media = statsMap["media"] ?: StatisticData(0, 0.0),
            culture = statsMap["culture"] ?: StatisticData(0, 0.0),
            publicAreas = statsMap["publicAreas"] ?: StatisticData(0, 0.0),
            environment = statsMap["environment"] ?: StatisticData(0, 0.0),
            other = statsMap["other"] ?: StatisticData(0, 0.0)
        )

        val totalCount = statsMap.values
            .sumOf { it.count ?: 0 }

        val totalTime = statsMap.values
            .sumOf { it.totalTimeSpent ?: 0.0 }

        stats.total = StatisticData(
            count = totalCount,
            totalTimeSpent = totalTime
        )

        return stats
    }

    companion object {
        val CODE_TO_GROUP = mapOf(
            "IT" to "other",
            "LAYOUT" to "other",
            "FORMS" to "other",
            "SCRAPERS" to "other",
            "BOTS" to "other",
            "APPS" to "other",
            "LAWS" to "socialSecurity",
            "HR" to "other",
            "DESIGN_3D" to "other",
            "DATA_COLLECTION" to "other",
            "RESEARCH" to "other",
            "POSTING" to "other",
            "ARTICLES" to "other",
            "SMM" to "media",
            "DESIGN" to "media",
            "WEB_FIGMA" to "media",
            "SOCIAL_MEDIA_DESIGN" to "media",
            "MATERIAL_STYLE" to "media",
            "STICKERS" to "media",
            "GUIDE_ILLUSTRATIONS" to "media",
            "BOOK_MURALS" to "media",
            "VIDEO_EDITING" to "media",
            "PHOTO" to "media",
            "GUIDEBOOK" to "media",
            "TRAINING" to "media",
            "COURSE_UPLOAD" to "media",
            "PSYCHOLOGISTS" to "media",
            "BOOK" to "culture",
            "LANGUAGE_COURSES" to "other",
            "SPORT" to "other",
            "MONITORING" to "publicAreas",
            "CLEAN_CITY" to "environment"
        )
    }
}
