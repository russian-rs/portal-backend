package rs.russian.portal.note.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import org.hibernate.annotations.SQLRestriction
import rs.russian.portal.note.domain.enums.EntityType
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

@Entity
@SQLRestriction("deleted = false")
class Note(
    @Id
    override var id: UUID? = UUID.randomUUID(),
    override var version: LocalDateTime? = null,

    var createTime: OffsetDateTime = OffsetDateTime.now(),

    var createdBy: String,

    var entityId: UUID,

    @Enumerated(STRING)
    var entityType: EntityType,

    var text: String,

    var deleted: Boolean = false,

    ) : JpaEntity<UUID>() {

    override fun equalityProperties() = setOf(Note::id, Note::text)
}
