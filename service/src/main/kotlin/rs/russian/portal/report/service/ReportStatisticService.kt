package rs.russian.portal.report.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.CityStatItem
import rs.russian.generated.model.CityStatistics
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

    /**
     * Volunteers per settlement for [year], same year semantics as [getStatistics]: a volunteer counts if any
     * of their contracts overlaps the year.
     *
     * `withoutCityCount` is derived as the remainder rather than counted separately: the breakdown starts from
     * `user_info` while [getTotalUserCount] starts from `account`, so subtracting keeps the two consistent even
     * for volunteers with no `user_info` row at all. The remainder is coerced at zero as a guard: every
     * volunteer lands in at most one group, so the sum cannot exceed the total unless a `user_info.city` value
     * resolves to two dictionary rows at once — which needs two entries sharing a normalized name.
     */
    @Transactional(readOnly = true)
    fun getCityStatistics(year: Int): CityStatistics {
        val yearStart = LocalDate.of(year, 1, 1)
        val yearEnd = yearStart.plusYears(1).minusDays(1)

        val rows = accountRepository.countVolunteersByCity(yearStart, yearEnd)
        val totalUsers = getTotalUserCount(yearStart, yearEnd)

        val items = rows.map { r ->
            CityStatItem(
                code = r.cityCode,
                name = r.cityName,
                nameCyrillic = r.cityNameCyrillic,
                count = r.volunteerCount
            )
        }

        return CityStatistics(
            year = year,
            items = items.toMutableList(),
            totalCount = totalUsers,
            withoutCityCount = (totalUsers - items.sumOf { it.count }).coerceAtLeast(0)
        )
    }

    /**
     * KNOWN LIMITATION — demographic drift from depersonalization (accepted trade-off).
     *
     * Depersonalization ([rs.russian.portal.user.service.AccountDepersonalizationService]) nulls a
     * volunteer's `gender` and `birthDate` but keeps their `username` and contracts. As a result, for any
     * year that included a since-depersonalized volunteer:
     *  - [totalUsers] (counted by DISTINCT username over overlapping contracts) STILL counts them;
     *  - the gender breakdown ([getGenderStatistics] groups by gender) drops them into a `null` bucket that
     *    is not surfaced as male/female;
     *  - the age slices ([getAgeSliceStatistics] filters `birth_date IS NOT NULL`) exclude them entirely.
     *
     * So `maleCount + femaleCount` and `Σ(age slices)` will be LESS than [totalUsers], and historical years
     * shrink retroactively as volunteers are depersonalized over time. This is accepted: personal-data
     * erasure takes precedence over demographic reconciliation. Do not "fix" it by un-nulling those fields.
     */
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
            .filter { it.groupCode == "KULTURNA_DOBRA" }
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
