package rs.russian.portal.report.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.*
import rs.russian.portal.report.mapper.HeatMapMapper
import rs.russian.portal.report.repository.ReportHeatMapRepository
import rs.russian.portal.shared.jpa.convert
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.mapper.UserMapper
import rs.russian.portal.user.service.AccountService
import java.math.BigDecimal.ZERO
import java.math.BigDecimal.valueOf
import java.time.LocalDate
import java.time.LocalDate.now

@Service
class HeatMapService(
    private val userMapper: UserMapper,
    private val userService: AccountService,
    private val heatMapMapper: HeatMapMapper,
    private val reportHeatMapRepository: ReportHeatMapRepository,
) {

    @Transactional(readOnly = true)
    fun getCurrentUserHeatMap(): Map<String, VolunteerHeatMapItem> {
        val account = userService.getCurrentAccount()
        val currentYear = now().year
        val previousYear = currentYear - 1
        val currentYearData = reportHeatMapRepository.findVolunteerHeatmap(
            usernames = setOf(account.username),
            startDate = LocalDate.of(currentYear, 1, 1),
            endDate = now()
        )
        val previousYearData = reportHeatMapRepository.findVolunteerHeatmap(
            usernames = setOf(account.username),
            startDate = LocalDate.of(previousYear, 1, 1),
            endDate = LocalDate.of(previousYear, 12, 31)
        )

        return mapOf(
            currentYear.toString() to createHeatMapItem(account, heatMapMapper.map(currentYearData)),
            previousYear.toString() to createHeatMapItem(account, heatMapMapper.map(previousYearData))
        )
    }

    @Transactional(readOnly = true)
    fun getHeatMap(
        searchQuery: String,
        pageRequest: PageRequest,
        filter: ReportsHeatMapFilter,
    ): ReportsHeatMapPageResponse {
        val accounts = userService.search(
            searchQuery,
            pageRequest,
            UserSearchFilter(program = filter.program, project = filter.project, onlyActive = true)
        )
        val year = filter.year ?: now().year
        val data = reportHeatMapRepository.findVolunteerHeatmap(
            usernames = accounts.map { it.username }.toSet(),
            startDate = LocalDate.of(year, 1, 1),
            endDate = if (now().year == year) now() else LocalDate.of(year, 12, 31)
        )
        val heatMap = HashMap<String, MutableList<HeatMapItem>>()
        data.forEach { row ->
            val weekItem = heatMapMapper.map(row)
            heatMap.merge(row.username, mutableListOf(weekItem)) { old, new -> (old + new).toMutableList() }
        }
        val result = ArrayList<VolunteerHeatMapItem>()
        accounts.forEach { account -> result.add(createHeatMapItem(account, heatMap[account.username])) }
        return ReportsHeatMapPageResponse(
            content = result.sortedByDescending { r -> r.totalRequired!!.minus(r.totalWorked!!) }.toMutableList(),
            page = convert(accounts),
        )
    }

    private fun createHeatMapItem(account: Account, weekItems: List<HeatMapItem>?): VolunteerHeatMapItem {
        val weeks = weekItems ?: emptyList()
        val totalRequired = weeks.sumOf { it.hoursRequired }
        return VolunteerHeatMapItem(
            volunteerInfo = userMapper.map(account.info),
            weeks = weeks.toMutableList(),
            totalWorked = weeks.sumOf { it.hoursWorked },
            totalRequired = if (totalRequired == ZERO) ZERO else totalRequired.minus(valueOf(10)),
        )
    }
}
