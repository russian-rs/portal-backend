package rs.russian.portal.report.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.ReportExternalApi
import rs.russian.generated.model.CreateReportRequest
import rs.russian.generated.model.ReportView
import rs.russian.portal.report.mapper.ReportMapper
import rs.russian.portal.report.service.ReportExternalService
import rs.russian.portal.shared.security.AuthorizedService

@RestController
@RequestMapping("/ext")
class ReportExternalController(
    private val reportMapper: ReportMapper,
    private val reportService: ReportExternalService,
) : ReportExternalApi {

    @AuthorizedService
    override fun createReport(createReportRequest: CreateReportRequest): ResponseEntity<ReportView> {
        val report = reportService.createReport(createReportRequest)
        return ResponseEntity.ok(reportMapper.mapView(report))
    }
}
