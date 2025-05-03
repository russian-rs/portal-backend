package rs.russian.portal.report.service

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import rs.russian.generated.model.ReportFilter
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.Task
import rs.russian.portal.user.service.AccountService
import rs.russian.portal.config.DefaultUserFilter
import org.springframework.security.core.context.SecurityContextHolder
import rs.russian.portal.report.domain.enums.ReportStatus
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("local", "no-auth", "test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportServiceTest {

    @Autowired
    lateinit var reportService: ReportService

    @Autowired
    lateinit var accountService: AccountService

    @Autowired
    lateinit var defaultUserFilter: DefaultUserFilter

    private lateinit var accountLogin: String

    @BeforeAll
    fun setup() {
        SecurityContextHolder.getContext().authentication =
            defaultUserFilter.getDefaultOAuth2Token()

        val account = accountService.findAccountByLogin(DefaultUserFilter.USERNAME)
            ?: throw IllegalStateException("Default user not created")
        accountLogin = account.username

        accountService.setProgram(account, "IT")

        val report = Report(account = account, status = ReportStatus.CREATED)
        report.tasks.add(Task(
            date = LocalDate.now(),
            name = "Test task 1",
            description = "First",
            timeSpent = 10,
            report = report
        ))
        report.tasks.add(Task(
            date = LocalDate.now(),
            name = "Test task 2",
            description = "Second",
            timeSpent = 20,
            report = report
        ))
        reportService.save(report)
    }

    @Test
    fun `service returns correct page of ReportDto`() {
        val filter = ReportFilter().apply {
            program = "IT"
            login = accountLogin
        }
        val page = reportService.getReports(filter, PageRequest.of(0, 10))

        assert(page.totalElements == 1L)
        val dto = page.content.first()
        assert(dto.tasks.size == 2)
        assert(dto.status.name == "CREATED")
    }
}
