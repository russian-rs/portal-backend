package rs.russian.portal.user.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserTokenRepository: JpaRepository<UserToken, Long> {

    fun findByToken(token: String): UserToken?
}
