package rs.russian.portal.report.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rs.russian.portal.report.repository.ReportRepository
import rs.russian.portal.report.repository.projections.ProgramStatProjection
import rs.russian.portal.user.domain.enums.Gender
import rs.russian.portal.user.repository.AccountRepository
import rs.russian.portal.user.repository.projections.AgeSliceCountProjection
import rs.russian.portal.user.repository.projections.GenderCountProjection
import rs.russian.portal.user.repository.projections.UsersStatisticGroupCountProjection
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ReportStatisticServiceTest {

    private lateinit var reportRepository: ReportRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var service: ReportStatisticService

    @BeforeEach
    fun setUp() {
        reportRepository = mockk()
        accountRepository = mockk()
        service = ReportStatisticService(reportRepository, accountRepository)
    }

    @Test
    fun `getStatistics should use users active during selected calendar year for totals`() {
        val year = 2025
        val yearStart = LocalDate.of(year, 1, 1)
        val yearEnd = LocalDate.of(year, 12, 31)

        every { reportRepository.fetchProgramStatsByGroup(any(), any()) } returns emptyList()
        every { accountRepository.countByGender(yearStart, yearEnd) } returns listOf(
            genderProjection(Gender.MALE, 3),
            genderProjection(Gender.FEMALE, 2)
        )
        every { accountRepository.countByAgeSlices(yearStart, yearEnd) } returns ageSliceProjection(1, 1, 1, 1, 1)
        every { accountRepository.countByStatisticGroup(yearStart, yearEnd) } returns listOf(
            statisticGroupProjection("KULTURNA_DOBRA", 2),
            statisticGroupProjection("ZIVOTNA_SREDINA", 1)
        )
        every { accountRepository.countByActiveDuringYear(yearStart, yearEnd) } returns 5L

        val result = service.getStatistics(year)
        val finalUsersStatistics = assertNotNull(result.finalUsersStatistics)
        val volunteerStatistics = assertNotNull(result.volunteerStatistics)

        assertEquals(5, finalUsersStatistics.totalCount)
        assertEquals(2, finalUsersStatistics.otherCount)
        assertEquals(5, volunteerStatistics.foreignersCount)
        verify(exactly = 1) { accountRepository.countByGender(yearStart, yearEnd) }
        verify(exactly = 1) { accountRepository.countByAgeSlices(yearStart, yearEnd) }
        verify(exactly = 1) { accountRepository.countByStatisticGroup(yearStart, yearEnd) }
        verify(exactly = 2) { accountRepository.countByActiveDuringYear(yearStart, yearEnd) }
    }

    @Test
    fun `getStatistics should map volunteer slices and genders`() {
        val yearStart = LocalDate.of(2025, 1, 1)
        val yearEnd = LocalDate.of(2025, 12, 31)

        every { reportRepository.fetchProgramStatsByGroup(any(), any()) } returns emptyList()
        every { accountRepository.countByGender(yearStart, yearEnd) } returns listOf(
            genderProjection(Gender.MALE, 7),
            genderProjection(Gender.FEMALE, 4)
        )
        every { accountRepository.countByAgeSlices(yearStart, yearEnd) } returns ageSliceProjection(2, 3, 4, 5, 6)
        every { accountRepository.countByStatisticGroup(yearStart, yearEnd) } returns emptyList()
        every { accountRepository.countByActiveDuringYear(yearStart, yearEnd) } returns 11L

        val result = service.getStatistics(2025)
        val volunteerStatistics = assertNotNull(result.volunteerStatistics)

        assertEquals(7, volunteerStatistics.maleCount)
        assertEquals(4, volunteerStatistics.femaleCount)
        assertEquals(2, volunteerStatistics.age15to18Count)
        assertEquals(3, volunteerStatistics.age18to30Count)
        assertEquals(4, volunteerStatistics.age30to40Count)
        assertEquals(5, volunteerStatistics.age40to65Count)
        assertEquals(6, volunteerStatistics.age65AndAboveCount)
    }

    @Test
    fun `getStatistics should request yearly program stats and aggregate totals`() {
        val capturedRanges = mutableListOf<Pair<OffsetDateTime, OffsetDateTime>>()
        val yearStart = LocalDate.of(2024, 1, 1)
        val yearEnd = LocalDate.of(2024, 12, 31)
        every { reportRepository.fetchProgramStatsByGroup(any(), any()) } answers {
            capturedRanges += firstArg<OffsetDateTime>() to secondArg<OffsetDateTime>()
            listOf(
                programProjection("IT", 3, 7.5),
                programProjection("MEDIA", 2, 2.0)
            )
        }
        every { accountRepository.countByGender(yearStart, yearEnd) } returns emptyList()
        every { accountRepository.countByAgeSlices(yearStart, yearEnd) } returns ageSliceProjection(0, 0, 0, 0, 0)
        every { accountRepository.countByStatisticGroup(yearStart, yearEnd) } returns emptyList()
        every { accountRepository.countByActiveDuringYear(yearStart, yearEnd) } returns 0L

        val result = service.getStatistics(2024)
        val programStatistics = assertNotNull(result.programStatistics)

        assertEquals(1, capturedRanges.size)
        assertEquals(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), capturedRanges[0].first)
        assertEquals(OffsetDateTime.of(2024, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.UTC), capturedRanges[0].second)

        assertEquals(2, programStatistics.items.size)
        assertEquals("IT", programStatistics.items[0].code)
        assertEquals(3, programStatistics.items[0].data.count)
        assertEquals(7.5, programStatistics.items[0].data.totalTimeSpent)
        assertEquals(5, programStatistics.total.count)
        assertEquals(9.5, programStatistics.total.totalTimeSpent)

        verify(exactly = 1) { accountRepository.countByGender(yearStart, yearEnd) }
        verify(exactly = 1) { accountRepository.countByAgeSlices(yearStart, yearEnd) }
        verify(exactly = 1) { accountRepository.countByStatisticGroup(yearStart, yearEnd) }
        verify(exactly = 2) { accountRepository.countByActiveDuringYear(yearStart, yearEnd) }
    }

    private fun programProjection(groupCode: String, count: Long, totalTimeSpent: Double): ProgramStatProjection =
        object : ProgramStatProjection {
            override val groupCode: String = groupCode
            override val count: Long = count
            override val totalTimeSpent: Double = totalTimeSpent
        }

    private fun ageSliceProjection(
        age15to18Count: Int,
        age18to30Count: Int,
        age30to40Count: Int,
        age40to65Count: Int,
        age65AndAboveCount: Int
    ): AgeSliceCountProjection =
        object : AgeSliceCountProjection {
            override val age15to18Count: Int = age15to18Count
            override val age18to30Count: Int = age18to30Count
            override val age30to40Count: Int = age30to40Count
            override val age40to65Count: Int = age40to65Count
            override val age65AndAboveCount: Int = age65AndAboveCount
        }

    private fun genderProjection(gender: Gender?, count: Int): GenderCountProjection =
        object : GenderCountProjection {
            override val gender: Gender? = gender
            override val count: Int = count
        }

    private fun statisticGroupProjection(groupCode: String, count: Int): UsersStatisticGroupCountProjection =
        object : UsersStatisticGroupCountProjection {
            override val groupCode: String = groupCode
            override val userCount: Int = count
        }
}
