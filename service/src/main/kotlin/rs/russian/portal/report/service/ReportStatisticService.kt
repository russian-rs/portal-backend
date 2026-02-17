package rs.russian.portal.report.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.FinalUsersStatistics
import rs.russian.generated.model.ProgramStatItem
import rs.russian.generated.model.ProgramStatistics
import rs.russian.generated.model.StatisticData
import rs.russian.generated.model.Statistics
import rs.russian.generated.model.VolunteerStatistics
import rs.russian.portal.report.repository.ReportRepository
import rs.russian.portal.user.domain.enums.Gender
import rs.russian.portal.user.repository.AccountRepository
import rs.russian.portal.user.repository.projections.AgeSliceCountProjection
import rs.russian.portal.user.repository.projections.UsersStatisticGroupCountProjection
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset


@Service
class ReportStatisticService(
    private val reportRepository: ReportRepository,
    private val accountRepository: AccountRepository
) {

    @Transactional(readOnly = true)
    fun getStatistics(year: Int) = Statistics().apply {
        val yearStart = LocalDate.of(year, 1, 1)
        val yearEnd = yearStart.plusYears(1).minusDays(1)

        programStatistics = getProgramStat(year)
        volunteerStatistics = getVolunteerStat(yearStart, yearEnd)
        finalUsersStatistics = getFinalUsersStat(yearStart, yearEnd)
        this.year = year
    }

    private fun getVolunteerStat(yearStart: LocalDate, yearEnd: LocalDate): VolunteerStatistics {
        val ageSlices = getAgeSliceStatistics(yearStart, yearEnd)
        val genderSlices = getGenderStatistics(yearStart, yearEnd)
        val totalUsers = getTotalUserCount(yearStart, yearEnd)

        return VolunteerStatistics().apply {
            maleCount = genderSlices.get(Gender.MALE)
            femaleCount = genderSlices.get(Gender.FEMALE)
            age15to18Count = ageSlices.age15to18Count
            age18to30Count = ageSlices.age18to30Count
            age30to40Count = ageSlices.age30to40Count
            age40to65Count = ageSlices.age40to65Count
            age65AndAboveCount = ageSlices.age65AndAboveCount
            //TODO(Add citizenship)
            citizensCount = 0
            foreignersCount = totalUsers
        }
    }

    private fun getFinalUsersStat(yearStart: LocalDate, yearEnd: LocalDate): FinalUsersStatistics {
        val usersByStatGroup = getCountByStatisticGroup(yearStart, yearEnd)
        val totalUsers = getTotalUserCount(yearStart, yearEnd)

        val culturalAssetsCount = usersByStatGroup
            .filter { it.groupCode == "KULTURNA_DOBA" }
            .sumOf { it.userCount }
        val naturalAssetsCount = usersByStatGroup
            .filter { it.groupCode == "ZIVOTNA_SREDINA" }
            .sumOf { it.userCount }
        val publicAreasCount = usersByStatGroup
            .filter { it.groupCode == "JAVNE_POVRSINE" }
            .sumOf { it.userCount }

        val other = totalUsers - (culturalAssetsCount + naturalAssetsCount + publicAreasCount)

        return FinalUsersStatistics().apply {
            this.culturalAssetsCount = culturalAssetsCount
            this.naturalAssetsCount = naturalAssetsCount
            this.publicAreasCount = publicAreasCount
            otherCount = other
            totalCount = totalUsers
        }
    }

    private fun getProgramStat(year: Int): ProgramStatistics {
        val start = OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val end = start.plusYears(1).minusNanos(1)

        val rows = reportRepository.fetchProgramStatsByGroup(start, end)

        val items = rows.map { r ->
            ProgramStatItem(
                code = r.groupCode,
                data = StatisticData(r.count.toInt(), r.totalTimeSpent)
            )
        }

        val total = StatisticData(
            rows.sumOf { it.count }.toInt(),
            rows.sumOf { it.totalTimeSpent }
        )

        return ProgramStatistics(items = items as MutableList<ProgramStatItem>, total = total)
    }

    fun getGenderStatistics(yearStart: LocalDate, yearEnd: LocalDate): Map<Gender?, Int> {
        return accountRepository.countByGender(yearStart, yearEnd).associate { it.gender to it.count }
    }

    fun getAgeSliceStatistics(yearStart: LocalDate, yearEnd: LocalDate): AgeSliceCountProjection {
        return accountRepository.countByAgeSlices(yearStart, yearEnd)
    }

    fun getTotalUserCount(yearStart: LocalDate, yearEnd: LocalDate): Int {
        return accountRepository.countByActiveDuringYear(yearStart, yearEnd).toInt()
    }

    fun getCountByStatisticGroup(yearStart: LocalDate, yearEnd: LocalDate): List<UsersStatisticGroupCountProjection> {
        return accountRepository.countByStatisticGroup(yearStart, yearEnd)
    }
}
