package rs.russian.portal.user.domain

import jakarta.persistence.*
import jakarta.persistence.EnumType.STRING
import rs.russian.portal.shared.jpa.JpaEntity
import rs.russian.portal.user.domain.enums.ContractType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
class Contract(
    @Id
    override var id: UUID? = UUID.randomUUID(),
    override var version: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "username", referencedColumnName = "username")
    var account: Account,

    var startDate: LocalDate,
    var endDate: LocalDate,

    @Enumerated(STRING)
    var type: ContractType = ContractType.REGULAR

) : JpaEntity<UUID>() {

    override fun equalityProperties() = setOf(Contract::id)
}
