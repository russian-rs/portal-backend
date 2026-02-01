package rs.russian.portal.user.domain

import jakarta.persistence.*
import rs.russian.portal.file.domain.FileInfo
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
class ResidencePermit(
    @Id
    override var id: UUID? = UUID.randomUUID(),
    override var version: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account,

    var nationality: String,
    @Column(length = 9)
    var registrationNumber: String,
    var validUntil: LocalDate,
    var purposeOfStay: String,
    var note: String?,
    @Column(length = 13)
    var identityNumber: String,
    var issuingDate: LocalDate,
    var issuingAuthority: String,
    var stateOfBirth: String,

    @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "front_side_file_id")
    var frontSidePhoto: FileInfo,

    @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "back_side_file_id")
    var backSidePhoto: FileInfo

) : JpaEntity<UUID>() {
    override fun equalityProperties() = setOf(ResidencePermit::id)
}
