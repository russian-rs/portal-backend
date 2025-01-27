package rs.russian.portal.report.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.NoteDto
import rs.russian.generated.model.ReportDto
import rs.russian.generated.model.ReportFilter
import rs.russian.portal.file.service.FileService
import rs.russian.portal.note.domain.Note
import rs.russian.portal.note.domain.enums.EntityType
import rs.russian.portal.note.service.NoteService
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.enums.ReportStatus
import rs.russian.portal.report.domain.specification.from
import rs.russian.portal.report.mapper.ReportMapper
import rs.russian.portal.report.repository.ReportRepository
import rs.russian.portal.shared.security.currentUserLogin
import rs.russian.portal.user.service.AccountService
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
        val currentAccount = accountService.getAccountByLogin(currentUserLogin())
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
            val currentAccount = accountService.getAccountByLogin(currentUserLogin())
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
}
