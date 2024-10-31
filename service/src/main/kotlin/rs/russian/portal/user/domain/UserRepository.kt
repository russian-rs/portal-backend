package rs.russian.portal.user.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<Account, String> {

    fun findByUsername(username: String): Optional<Account>
}
