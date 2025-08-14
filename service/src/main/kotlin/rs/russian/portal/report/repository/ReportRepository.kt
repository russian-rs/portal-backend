package rs.russian.portal.report.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.Report.Companion.GRAPH_FULL
import java.util.*

@Repository
interface ReportRepository : JpaRepository<Report, UUID> {

    @EntityGraph(value = GRAPH_FULL)
    override fun findById(id: UUID): Optional<Report>

    @EntityGraph(value = GRAPH_FULL)
    fun findAllByIdIn(values: Collection<UUID>, sort: Sort): List<Report>

    fun findAll(specification: Specification<Report>, pageable: Pageable): Page<Report>
}
