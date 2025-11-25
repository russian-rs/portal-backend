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
    fun getCurrentUserHeatMap(year: Int): VolunteerHeatMapItem {
        val account = userService.getCurrentAccount()
        val weeks = reportHeatMapRepository.findVolunteerHeatmap(
            usernames = setOf(account.username),
            startDate = LocalDate.of(year, 1, 1),
            endDate = LocalDate.of(year, 12, 31)
        )
        return createHeatMapItem(account, heatMapMapper.map(weeks))
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
        val startDate = filter.startDate ?: DEFAULT_START_DATE // Из фильтра или начало года
        val endDate = if (now().year == startDate.year) now() else LocalDate.of(startDate.year, 12, 31)
        val data = reportHeatMapRepository.findVolunteerHeatmap(
            usernames = accounts.map { it.username }.toSet(),
            startDate = startDate,
            endDate = endDate
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

    companion object {
        private val DEFAULT_START_DATE = LocalDate.of(now().year, 1, 1)
    }
}
