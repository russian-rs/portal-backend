package rs.russian.portal.user.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDateTime

/**
 * Stores refresh JWT token
 */
@Entity
@Table(name = "user_token")
class UserToken(
    @Id override var id: Long? = null,
    override var version: LocalDateTime? = null,
    val token: String,
    @ManyToOne val user: User
) : JpaEntity<Long>() {

    override fun equalityProperties() = setOf(UserToken::token)
}
