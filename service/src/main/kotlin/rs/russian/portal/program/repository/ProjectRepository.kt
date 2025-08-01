package rs.russian.portal.program.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import rs.russian.portal.program.domain.Project

@Repository
interface ProjectRepository : JpaRepository<Project, String> {
    fun findByCode(code: String): Project?
}
