package rs.russian.portal.report.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.FinalUsersStatistics
import rs.russian.generated.model.NoteDto
import rs.russian.generated.model.ProgramStatistics
import rs.russian.generated.model.ReportDto
import rs.russian.generated.model.ReportFilter
import rs.russian.generated.model.StatisticData
import rs.russian.generated.model.Statistics
import rs.russian.generated.model.VolunteerStatistics
import rs.russian.portal.file.service.FileService
import rs.russian.portal.note.domain.Note
import rs.russian.portal.note.domain.enums.EntityType
import rs.russian.portal.note.service.NoteService
import rs.russian.portal.program.domain.StatisticGroup
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.enums.ReportStatus
import rs.russian.portal.report.domain.specification.from
import rs.russian.portal.report.mapper.ReportMapper
import rs.russian.portal.report.repository.ReportRepository
import rs.russian.portal.shared.exception.NotAuthorizedException
import rs.russian.portal.shared.security.currentUserLogin
import rs.russian.portal.user.domain.enums.Gender
import rs.russian.portal.user.service.AccountService
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class ReportService(
    private val accountService: AccountService,
    private val fileService: FileService,
    private val reportMapper: ReportMapper,
    private val noteService: NoteService,
    private val reportRepository: ReportRepository
) {

    @Transactional(readOnly = true)
    fun getReport(reportId: UUID): Report {
        return reportRepository.findById(reportId).orElseThrow()
    }

    @Transactional
    fun createReport(reportDto: ReportDto): Report {
        val report = Report(account = accountService.getCurrentAccount(), status = ReportStatus.CREATED)
        val tasks = reportDto.tasks.map { taskDto ->
            reportMapper.map(taskDto, report).also { task ->
                task.customer = accountService.findAccountByLogin(taskDto.customer)
                task.files = fileService.findAllByIds(taskDto.files?.map { it.id }?.toSet())
            }
        }
        return reportRepository.save(report.also { it.tasks = tasks.toMutableSet() })
    }

    @Transactional
    fun deleteReport(reportId: UUID) {
        val report = getReport(reportId)
        reportRepository.save(report.also { it.deleted = true })
    }

    @Transactional
    fun updateReport(reportDto: ReportDto): Report {
        val report = getReport(reportDto.id)
        val tasks = reportDto.tasks.map { taskDto ->
            reportMapper.map(taskDto, report).also { task ->
                task.customer = accountService.findAccountByLogin(taskDto.customer)
                task.files = fileService.findAllByIds(taskDto.files?.map { it.id }?.toSet())
            }
        }
        report.status = ReportStatus.CREATED
        report.tasks.clear()
        return reportRepository.save(report.also { it.tasks.addAll(tasks) })
    }

    @Transactional(readOnly = true)
    fun getReports(reportFilter: ReportFilter, pageable: Pageable): Page<Report> {
        return reportRepository.findAll(from(reportFilter), pageable)
    }

    @Transactional
    fun save(report: Report): Report {
        return reportRepository.save(report)
    }

    @Transactional
    fun addNote(reportId: UUID, noteDto: NoteDto): Note {
        val report = getReport(reportId)
        val currentAccount = accountService.getAccountByLogin(currentUserLogin() ?: throw NotAuthorizedException())
        val note = noteService.save(
            Note(
                createdBy = currentAccount,
                entityId = reportId,
                entityType = EntityType.REPORT,
                text = noteDto.text
            )
        )
        report.notes.add(note)
        return note
    }

    @Transactional
    fun changeStatus(reportId: UUID, status: ReportStatus, noteText: String? = null) {
        val report = getReport(reportId)
        if (!noteText.isNullOrEmpty()) {
            val currentAccount = accountService.getAccountByLogin(currentUserLogin() ?: throw NotAuthorizedException())
            val note = noteService.save(
                Note(
                    createdBy = currentAccount,
                    entityId = reportId,
                    entityType = EntityType.REPORT,
                    text = noteText
                )
            )
            report.notes.add(note)
        }
        report.status = status
    }

    @Transactional(readOnly = true)
    fun getStatistics(year: Int): Statistics {

        return Statistics().apply {
            programStatistics = getProgramStat(year)
            volunteerStatistics = getVolunteerStat()
            finalUsersStatistics = getFinalUsersStat()
            this.year = year
        }
    }

    private fun getProgramStat(year: Int): ProgramStatistics {
        val start = OffsetDateTime.of(year,1,1,0,0,0,0, ZoneOffset.UTC)
        val end   = start.plusYears(1).minusNanos(1)

        val raw = reportRepository.fetchProgramStatsByGroup(start, end)
            .associateBy({ it.groupName }, { it })

        fun getData(key: StatisticGroup?) = raw[key]
            ?.let { StatisticData(it.count.toInt(), it.totalTimeSpent) }
            ?: StatisticData(0, 0.0)

        val other = getData(null)

        return ProgramStatistics().apply {
            socialSecurity = getData(StatisticGroup.SOCIJALNA_ZASTITA)
            media          = getData(StatisticGroup.MEDIJI_I_KOMUNIKACIJE)
            culture        = getData(StatisticGroup.KULTURNA_DOBA)
            publicAreas    = getData(StatisticGroup.JAVNE_POVRSINE)
            environment    = getData(StatisticGroup.ZIVOTNA_SREDINA)
            this.other     = other

            val totalCount = raw.values.sumOf { it.count }
            val totalTime  = raw.values.sumOf { it.totalTimeSpent }
            total = StatisticData(totalCount.toInt(), totalTime)
        }
    }

    private fun getVolunteerStat(): VolunteerStatistics {
        val ageSlices = accountService.getAgeSliceStatistics()
        val genderSlices = accountService.getGenderStatistics()
        val  totalUsers = accountService.getTotalUserCount()

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

    private fun getFinalUsersStat(): FinalUsersStatistics {
        val usersByStatGroup = accountService.getCountByStatisticGroup()
        val totalUsers = accountService.getTotalUserCount()

        val culturalAssetsCount = usersByStatGroup
            .filter { it.groupName == StatisticGroup.KULTURNA_DOBA }
            .sumOf { it.userCount }
        val naturalAssetsCount = usersByStatGroup
            .filter { it.groupName == StatisticGroup.ZIVOTNA_SREDINA }
            .sumOf { it.userCount }
        val publicAreasCount = usersByStatGroup
            .filter { it.groupName == StatisticGroup.JAVNE_POVRSINE }
            .sumOf { it.userCount }

        val other = totalUsers - (culturalAssetsCount + naturalAssetsCount + publicAreasCount)

        return FinalUsersStatistics().apply {
            this.culturalAssetsCount = culturalAssetsCount
            this.naturalAssetsCount = naturalAssetsCount
            this.publicAreasCount = publicAreasCount
            otherCount = other.toInt()
            totalCount = totalUsers.toInt()
        }
    }
}
