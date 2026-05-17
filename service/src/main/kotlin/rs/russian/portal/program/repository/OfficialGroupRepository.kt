package rs.russian.portal.program.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import rs.russian.portal.program.domain.OfficialGroup
import rs.russian.portal.program.domain.Project

@Repository
interface OfficialGroupRepository : JpaRepository<OfficialGroup, String> {
    fun findByCode(code: String): OfficialGroup?
}
