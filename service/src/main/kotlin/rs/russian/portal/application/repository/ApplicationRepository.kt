package rs.russian.portal.application.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import rs.russian.portal.application.domain.Application
import java.util.*

@Repository
interface ApplicationRepository : JpaRepository<Application, UUID> {

    fun findByEmail(email: String): Optional<Application>

    fun findByEmailAndPassport(email: String, passport: String): Optional<Application>
}
