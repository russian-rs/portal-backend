package rs.russian.portal.user.repository

import org.springframework.data.jpa.repository.JpaRepository
import rs.russian.generated.model.ProgramCode
import rs.russian.portal.user.domain.Program


interface ProgramRepository : JpaRepository<Program, ProgramCode> {
    fun findByCode(code: ProgramCode): Program?
}
