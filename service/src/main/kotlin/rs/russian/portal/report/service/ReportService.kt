package rs.russian.portal.report.service

import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
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
import rs.russian.portal.shared.exception.NotAuthorizedException
import rs.russian.portal.shared.security.currentUserLogin
import rs.russian.portal.user.service.AccountService
import java.util.*

@Service
class ReportService(
    private val accountService: AccountService,
    private val fileService: FileService,
    private val reportMapper: ReportMapper,
    private val noteService: NoteService,
    private val reportRepository: ReportRepository,
    private val entityManager: EntityManager,
    private val taskTranslationService: TaskTranslationService
) {

    @Transactional(readOnly = true)
    fun getReport(reportId: UUID): Report {
        return reportRepository.findById(reportId).orElseThrow()
    }

    @Transactional
    fun createReport(reportDto: ReportDto): Report {
        val currentAccount = accountService.getCurrentAccount()
        val report = Report(
            account = currentAccount,
            status = ReportStatus.CREATED,
            program = currentAccount.info?.program,
            project = currentAccount.info?.project
        )
        val tasks = reportDto.tasks.map { taskDto ->
            reportMapper.map(taskDto, report).also { task ->
                task.customer = accountService.findAccountByLogin(taskDto.customer)
                task.files = fileService.findAllByIds(taskDto.files?.map { it.id }?.toMutableSet())
                taskTranslationService.translateTask(task)
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
                taskTranslationService.translateTask(task)
            }
        }
        report.status = ReportStatus.CREATED
        report.tasks.clear()
        return reportRepository.save(report.also { it.tasks.addAll(tasks) })
    }

    @Transactional(readOnly = true)
    fun getReports(reportFilter: ReportFilter, pageable: Pageable): Page<Report> {
        return findAllFull(from(reportFilter), pageable)
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
                createdBy = currentAccount.username,
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
                    createdBy = currentAccount.username,
                    entityId = reportId,
                    entityType = EntityType.REPORT,
                    text = noteText
                )
            )
            report.notes.add(note)
        }
        report.status = status
    }

    /**
     * Получить список отчетов с использованием EntityGraph
     * Решает проблему, при которой невозможно одновременная работа Pageable и EntityGraph:
     * HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory
     * entityManager.detach(...) - необходим потому что findAll загружает "легковесные" объекты и
     * сохраняет их в контекст, а findAllByIdIn уже загрузит полные объекты, однако если не очистить
     * контекст, то они не будут перезаписаны в контексте, что повлечет дополнительные SQL запросы
     */
    private fun findAllFull(specification: Specification<Report>, pageable: Pageable): Page<Report> {
        val reports = reportRepository.findAll(specification, pageable)
        reports.forEach { report -> entityManager.detach(report) }
        val reportsFull = reportRepository.findAllByIdIn(reports.mapNotNull { it.id }, pageable.sort)
        return PageImpl(reportsFull, reports.pageable, reports.totalElements)
    }
}
