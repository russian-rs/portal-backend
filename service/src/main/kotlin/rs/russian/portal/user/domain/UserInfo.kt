package rs.russian.portal.user.domain

import jakarta.persistence.*
import jakarta.persistence.CascadeType.ALL
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.FetchType.LAZY
import rs.russian.portal.file.domain.FileInfo
import rs.russian.portal.program.domain.Program
import rs.russian.portal.program.domain.Project
import rs.russian.portal.shared.jpa.JpaEntity
import rs.russian.portal.user.domain.enums.Gender
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@NamedEntityGraph(
    name = UserInfo.GRAPH_FULL,
    attributeNodes = [
        NamedAttributeNode("avatar"),
        NamedAttributeNode("program"),
        NamedAttributeNode("project")
    ]
)
data class UserInfo(
    @Id
    @Column(name = "username")
    override var id: String? = null,
    override var version: LocalDateTime? = null,

    var city: String? = null,
    var address: String? = null,
    var birthDate: LocalDate? = null,
    var telegram: String? = null,
    var phone: String? = null,

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "program_code")
    var program: Program? = null,

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "project_code")
    var project: Project? = null,

    @Enumerated(STRING)
    var gender: Gender? = null,

    @OneToOne
    @MapsId("id")
    @JoinColumn(name = "username", referencedColumnName = "username")
    var account: Account,

    @OneToOne(fetch = LAZY, cascade = [ALL], orphanRemoval = true)
    @JoinColumn(name = "avatar_file_id")
    var avatar: FileInfo? = null

) : JpaEntity<String>() {

    override fun equalityProperties() = setOf(UserInfo::id)

    companion object {

        const val GRAPH_FULL = "UserInfoFull"

        fun default(account: Account) = UserInfo(
            id = account.username,
            account = account,
            city = "Beograd",
            birthDate = LocalDate.of(2000, 1, 1)
        )
    }
}
