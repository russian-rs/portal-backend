package rs.russian.portal.report.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.ProgramStatistics
import rs.russian.generated.model.StatisticData
import rs.russian.generated.model.Statistics
import rs.russian.portal.report.repository.ReportRepository
import rs.russian.portal.report.repository.projections.ProgramStatProjection
import java.time.OffsetDateTime
import java.time.ZoneOffset


@Service
class ReportStatisticService(
    private val reportRepository: ReportRepository
) {

    @Transactional(readOnly = true)
    fun getStatistics(year: Int) = Statistics().apply {
        programStatistics = getProgramStat(year)
        this.year = year
    }


    private fun getProgramStat(year: Int): ProgramStatistics {
        val start = OffsetDateTime.of(
            year, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        val end = start.plusYears(1).minusNanos(1)

        val rawByCode: Map<String?, ProgramStatProjection> =
            reportRepository.fetchProgramStatsByGroup(start, end)
                .associateBy({ it.groupCode }, { it })

        fun getData(code: String?) = rawByCode[code]
            ?.let { StatisticData(it.count.toInt(), it.totalTimeSpent) }
            ?: StatisticData(0, 0.0)

        return ProgramStatistics().apply {
            socialSecurity = getData("SOCIJALNA_ZASTITA")
            media          = getData("MEDIJI_I_KOMUNIKACIJE")
            culture        = getData("KULTURNA_DOBA")
            publicAreas    = getData("JAVNE_POVRSINE")
            environment    = getData("ZIVOTNA_SREDINA")
            this.other     = getData(null)

            val totalCount = rawByCode.values.sumOf { it.count }
            val totalTime  = rawByCode.values.sumOf { it.totalTimeSpent }
            total = StatisticData(totalCount.toInt(), totalTime)
        }
    }
}