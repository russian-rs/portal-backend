package rs.russian.portal.program.repository

import org.springframework.data.jpa.repository.JpaRepository
import rs.russian.portal.program.domain.Program


interface ProgramRepository : JpaRepository<Program, String> {
    fun findByCode(code: String): Program?
}
