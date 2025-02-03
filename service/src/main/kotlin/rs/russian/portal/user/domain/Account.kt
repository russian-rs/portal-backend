package rs.russian.portal.user.domain

import jakarta.persistence.*
import jakarta.persistence.CascadeType.ALL
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import rs.russian.portal.report.domain.Report
import rs.russian.portal.shared.jpa.JpaEntity
import rs.russian.portal.shared.jpa.converter.UserGroupSetConverter
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.domain.listener.AccountEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AccountEntityListener::class)
@NamedEntityGraph(
    name = Account.GRAPH_FULL,
    attributeNodes = [
        NamedAttributeNode("info", subgraph = UserInfo.GRAPH_AVATAR),
        NamedAttributeNode("contracts"),
    ],
    subgraphs = [NamedSubgraph(name = UserInfo.GRAPH_AVATAR, attributeNodes = [NamedAttributeNode("avatar")])]
)
data class Account(
    @Id
    override var id: Int? = null,
    override var version: LocalDateTime? = null,

    var username: String,
    var email: String,
    var fullName: String,

    @OneToOne(mappedBy = "account", cascade = [ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var info: UserInfo? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = UserGroupSetConverter::class)
    var groups: Set<UserGroup> = mutableSetOf(),

    @OneToMany(mappedBy = "account", cascade = [ALL], orphanRemoval = true)
    var reports: List<Report> = ArrayList(),

    @OneToMany(mappedBy = "account", cascade = [ALL], orphanRemoval = true)
    var contracts: List<Contract> = ArrayList(),

    var active: Boolean = true,

    var lastSynced: LocalDateTime? = null,

    ) : JpaEntity<Int>() {

    override fun equalityProperties() = setOf(Account::email, Account::username)

    companion object {
        const val GRAPH_FULL = "AccountUserInfoContracts"
    }
}
