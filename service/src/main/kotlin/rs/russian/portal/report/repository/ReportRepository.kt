package rs.russian.portal.report.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.Report.Companion.GRAPH_TASKS_ACCOUNT
import java.util.*

interface ReportRepository : JpaRepository<Report, UUID> {

    @EntityGraph(value = GRAPH_TASKS_ACCOUNT)
    override fun findById(id: UUID): Optional<Report>

    @EntityGraph(value = GRAPH_TASKS_ACCOUNT)
    fun findAll(specification: Specification<Report>, pageable: Pageable): Page<Report>

    @EntityGraph(value = GRAPH_TASKS_ACCOUNT)
    fun findAllByAccountUsername(username: String, pageable: Pageable): Page<Report>

    @EntityGraph(value = GRAPH_TASKS_ACCOUNT)
    fun findByAccountUsernameAndHash(username: String, hash: Int): Optional<Report>
}
