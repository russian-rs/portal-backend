package rs.russian.portal.report.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.Report.Companion.GRAPH_FULL
import rs.russian.portal.report.repository.projections.ProgramStatProjection
import java.time.OffsetDateTime
import java.util.*

interface ReportRepository : JpaRepository<Report, UUID> {

    @EntityGraph(value = GRAPH_FULL)
    override fun findById(id: UUID): Optional<Report>

    @EntityGraph(value = GRAPH_FULL)
    fun findAll(specification: Specification<Report>, pageable: Pageable): Page<Report>

    @EntityGraph(value = GRAPH_FULL)
    fun findByCreateTimeBetween(from: OffsetDateTime, to: OffsetDateTime): List<Report>

    @Query(
        """
  SELECT
    g.code AS groupCode,
    COUNT(DISTINCT r.account.username) AS count,
    COALESCE(SUM(t.timeSpent), 0) / 60.0 AS totalTimeSpent
  FROM Report r
  LEFT JOIN r.tasks t
  LEFT JOIN r.account.info.project.statisticGroups g
  WHERE r.createTime BETWEEN :start AND :end
  GROUP BY g.code
"""
    )
    fun fetchProgramStatsByGroup(
        @Param("start") start: OffsetDateTime,
        @Param("end") end: OffsetDateTime
    ): List<ProgramStatProjection>
}
