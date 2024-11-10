package rs.russian.portal.user.domain

import jakarta.persistence.*
import jakarta.persistence.CascadeType.ALL
import rs.russian.portal.report.domain.Report
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDateTime

@Entity
@NamedEntityGraph(
    name = Account.GRAPH_INFO,
    attributeNodes = [NamedAttributeNode("info")]
)
data class Account(
    @Id
    override var id: String? = null,
    override var version: LocalDateTime? = null,

    var username: String,
    var email: String,
    var fullName: String,

    @PrimaryKeyJoinColumn
    @OneToOne(cascade = [ALL], fetch = FetchType.LAZY)
    var info: UserInfo?,

    @OneToMany(mappedBy = "account", cascade = [ALL], orphanRemoval = true)
    var reports: List<Report> = ArrayList()

) : JpaEntity<String>() {

    override fun equalityProperties() = setOf(Account::email)

    companion object {
        const val GRAPH_INFO = "UserInfo"
    }
}
