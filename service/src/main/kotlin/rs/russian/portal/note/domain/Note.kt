package rs.russian.portal.note.domain

import jakarta.persistence.*
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.FetchType.LAZY
import rs.russian.portal.note.domain.enums.EntityType
import rs.russian.portal.shared.jpa.JpaEntity
import rs.russian.portal.user.domain.Account
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

@Entity
class Note(
    @Id
    override var id: UUID? = UUID.randomUUID(),
    override var version: LocalDateTime? = null,

    var createTime: OffsetDateTime = OffsetDateTime.now(),

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "username")
    var createdBy: Account,

    var entityId: UUID,

    @Enumerated(STRING)
    var entityType: EntityType,

    var text: String

) : JpaEntity<UUID>() {

    override fun equalityProperties() = setOf(Note::entityId, Note::entityType, Note::createdBy, Note::text)
}
