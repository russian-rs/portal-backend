package rs.russian.portal.report.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.CreateReportRequest
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.enums.ReportStatus
import rs.russian.portal.report.mapper.ReportMapper
import rs.russian.portal.report.repository.ReportRepository
import rs.russian.portal.shared.exception.InvalidRequestException
import rs.russian.portal.user.service.AccountService
import java.time.LocalDate

@Service
class ReportExternalService(
    private val reportMapper: ReportMapper,
    private val accountService: AccountService,
    private val reportRepository: ReportRepository,
) {

    @Transactional
    fun createReport(request: CreateReportRequest): Report {
        val account = accountService.getAccountByLogin(request.user)
        val report = Report(
            isAuto = true,
            account = account,
            status = ReportStatus.CREATED,
            program = account.info?.program,
            project = account.info?.project
        )
        val tasks = request.tasks.map { createTaskRequest ->
            if (createTaskRequest.date.isAfter(LocalDate.now())) {
                throw InvalidRequestException("Task date (${createTaskRequest.date}) must be in the past")
            }
            reportMapper.map(createTaskRequest, report)
        }
        return reportRepository.save(report.also { it.tasks = tasks.toMutableSet() })
    }
}
