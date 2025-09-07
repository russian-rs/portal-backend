package rs.russian.portal.report.repository.projections

interface ProgramStatProjection {
    val groupCode: String?
    val count: Long
    val totalTimeSpent: Double
}