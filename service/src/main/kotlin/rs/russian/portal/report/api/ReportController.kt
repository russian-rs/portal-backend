package rs.russian.portal.report.api

import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.ReportApi
import rs.russian.generated.model.PageRequest
import rs.russian.generated.model.ReportDto
import rs.russian.generated.model.ReportFilter
import rs.russian.generated.model.ReportPageResponse
import rs.russian.portal.report.mapper.ReportMapper
import rs.russian.portal.report.service.ReportService
import rs.russian.portal.shared.jpa.convert
import java.util.*

@RestController
class ReportController(
    private val reportService: ReportService,
    private val reportMapper: ReportMapper
) : ReportApi {

    @Transactional(readOnly = true)
    override fun getReport(id: UUID): ResponseEntity<ReportDto> {
        val report = reportService.getReport(id)
        return ResponseEntity.ok(reportMapper.map(report))
    }

    @Transactional
    override fun createReport(reportDto: ReportDto): ResponseEntity<ReportDto> {
        val report = reportService.createReport(reportDto)
        return ResponseEntity.ok(reportMapper.map(report))
    }

    @Transactional(readOnly = true)
    override fun getReports(pageRequest: PageRequest, reportFilter: ReportFilter): ResponseEntity<ReportPageResponse> {
        val page = reportService.getReports(reportFilter, convert(pageRequest))
        return ResponseEntity.ok(
            ReportPageResponse(
                page = convert(page),
                content = page.map { reportMapper.map(it) }.toMutableList()
            )
        )
    }

}
