package rs.russian.portal.project.repository

import org.springframework.data.jpa.repository.JpaRepository
import rs.russian.portal.project.domain.Project

interface ProjectRepository : JpaRepository<Project, String> {
    fun findByCode(code: String): Project?
}