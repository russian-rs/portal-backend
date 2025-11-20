package rs.russian.portal.application.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import rs.russian.portal.application.domain.Application
import rs.russian.portal.application.domain.Application.Companion.GRAPH_FULL
import rs.russian.portal.application.domain.ApplicationStatus
import java.util.*

@Repository
interface ApplicationRepository : JpaRepository<Application, UUID> {

    @EntityGraph(GRAPH_FULL)
    override fun findById(id: UUID): Optional<Application>

    @EntityGraph(GRAPH_FULL)
    fun findByEmail(email: String): Optional<Application>

    @EntityGraph(GRAPH_FULL)
    fun findByEmailAndStatusNotIn(email: String, statuses: List<ApplicationStatus>): Optional<Application>

    @EntityGraph(GRAPH_FULL)
    fun findByEmailAndPassportAndStatusNotIn(
        email: String,
        passport: String,
        statuses: List<ApplicationStatus>,
    ): Optional<Application>

    @EntityGraph(GRAPH_FULL)
    fun findAllByIdIn(idList: Collection<UUID>, sort: Sort): List<Application>

    fun findAll(specification: Specification<Application>, pageable: Pageable): Page<Application>
}
