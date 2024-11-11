package rs.russian.portal.user.domain

import jakarta.persistence.*
import jakarta.persistence.EnumType.STRING
import rs.russian.portal.file.domain.FileInfo
import rs.russian.portal.shared.enums.Program
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@NamedEntityGraph(
    name = UserInfo.GRAPH_AVATAR,
    attributeNodes = [NamedAttributeNode("avatar")]
)
data class UserInfo(
    @Id
    @Column(name = "user_id")
    override var id: String? = null,
    override var version: LocalDateTime? = null,

    var city: String? = null,
    var address: String? = null,
    var birthDate: LocalDate? = null,
    var telegram: String? = null,
    var phone: String? = null,

    @Enumerated(STRING)
    var program: Program? = null,

    @MapsId
    @OneToOne(mappedBy = "info")
    @JoinColumn(name = "user_id")
    var account: Account,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_file_id")
    var avatar: FileInfo? = null

) : JpaEntity<String>() {

    override fun equalityProperties() = setOf(UserInfo::id)

    companion object {

        const val GRAPH_AVATAR = "UserInfoAvatar"
    }
}
