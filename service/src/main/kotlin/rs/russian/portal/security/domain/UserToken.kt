package rs.russian.portal.security.domain

import jakarta.persistence.*
import jakarta.persistence.EnumType.*
import rs.russian.portal.shared.jpa.JpaEntity
import rs.russian.portal.user.domain.UserProfile
import java.time.LocalDateTime
import java.util.*

/**
 * Stores JWT token
 */
@Entity
class UserToken(
    @Id override var id: Long? = null,
    override var version: LocalDateTime? = null,
    val token: String,
    val validUntil: Date,
    @Enumerated(STRING) val type: TokenType,
    @ManyToOne val user: UserProfile
) : JpaEntity<Long>() {

    override fun equalityProperties() = setOf(UserToken::token, UserToken::type)
}
