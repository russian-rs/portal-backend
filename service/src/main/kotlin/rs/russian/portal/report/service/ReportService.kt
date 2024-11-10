package rs.russian.portal.report.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.ReportDto
import rs.russian.generated.model.ReportFilter
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.specification.from
import rs.russian.portal.report.mapper.ReportMapper
import rs.russian.portal.report.repository.ReportRepository
import rs.russian.portal.user.service.AccountService
import java.util.*

@Service
class ReportService(
    private val accountService: AccountService,
    private val reportMapper: ReportMapper,
    private val reportRepository: ReportRepository
) {

    @Transactional(readOnly = true)
    fun getReport(reportId: UUID): Report {
        return reportRepository.findById(reportId).orElseThrow()
    }

    @Transactional
    fun createReport(reportDto: ReportDto): Report {
        val report = Report(account = accountService.getCurrentUser())
        val tasks = reportDto.tasks.map { reportMapper.map(it, report) }
        return reportRepository.save(report.also { it.tasks = tasks })
    }

    @Transactional(readOnly = true)
    fun getReports(login: String, pageable: Pageable): Page<Report> {
        return reportRepository.findAllByAccountUsername(login, pageable)
    }

    @Transactional(readOnly = true)
    fun getReports(reportFilter: ReportFilter, pageable: Pageable): Page<Report> {
        return reportRepository.findAll(from(reportFilter), pageable)
    }
}
