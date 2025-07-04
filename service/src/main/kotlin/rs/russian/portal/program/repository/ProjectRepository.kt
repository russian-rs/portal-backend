package rs.russian.portal.program.repository

import org.springframework.data.jpa.repository.JpaRepository
import rs.russian.portal.program.domain.Project

interface ProjectRepository : JpaRepository<Project, String> {
    fun findByCode(code: String): Project?
}