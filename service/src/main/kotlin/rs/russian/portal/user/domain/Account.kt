package rs.russian.portal.user.domain

import jakarta.persistence.*
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDateTime

@Entity
data class Account(
    @Id
    override var id: String? = null,
    override var version: LocalDateTime? = null,

    var username: String,
    var email: String,
    var fullName: String,

    @PrimaryKeyJoinColumn
    @OneToOne(cascade = [CascadeType.ALL])
    var info: UserInfo?
) : JpaEntity<String>() {

    override fun equalityProperties() = setOf(Account::email)
}
