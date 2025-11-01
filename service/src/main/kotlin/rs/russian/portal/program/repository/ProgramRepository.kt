package rs.russian.portal.program.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import rs.russian.portal.program.domain.Program

@Repository
interface ProgramRepository : JpaRepository<Program, String> {
    fun findByCode(code: String): Program?

    @EntityGraph(attributePaths = ["projects"])
    override fun findAll(): List<Program>
}
