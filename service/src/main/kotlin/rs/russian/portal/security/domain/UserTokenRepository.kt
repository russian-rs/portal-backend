package rs.russian.portal.security.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import rs.russian.portal.user.domain.UserProfile
import java.util.*

@Repository
interface UserTokenRepository: JpaRepository<UserToken, Long> {

    fun existsByToken(token: String): Boolean

    fun deleteByToken(token: String)

    fun deleteAllByUser(user: UserProfile)

    fun deleteAllByValidUntilBefore(before: Date = Date())
}
