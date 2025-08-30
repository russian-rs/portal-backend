package rs.russian.portal.report.api

import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.StatisticsApi
import rs.russian.generated.model.Statistics
import rs.russian.portal.report.service.ReportStatisticService
import rs.russian.portal.shared.security.Authorized
import rs.russian.portal.user.domain.enums.UserGroup.ADMIN_VOLUNTEER

@RestController
class StatisticsController(
    private val reportStatisticService: ReportStatisticService
): StatisticsApi {

    @Transactional(readOnly = true)
    @Authorized(allowed = [ADMIN_VOLUNTEER])
    override fun getStatistics(year: Int): ResponseEntity<Statistics> {
        return ResponseEntity.ok(reportStatisticService.getStatistics(year))
    }
}