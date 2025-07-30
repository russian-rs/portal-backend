package rs.russian.portal.report.repository.projections

import rs.russian.portal.program.domain.StatisticGroup

interface ProgramStatProjection {
    val groupName: StatisticGroup?
    val count: Long
    val totalTimeSpent: Double
}