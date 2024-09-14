package rs.russian.portal.user.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDateTime

@Entity
data class UserProfile(
    @Id override var id: String? = null,
    override var version: LocalDateTime? = null,
    var username: String,
    var email: String,
    var fullName: String
) : JpaEntity<String>() {

    override fun equalityProperties() = setOf(UserProfile::email)
}
