package rs.russian.portal.announcement.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@Entity
class AnnouncementRead(
    @Id
    override var id: UUID? = UUID.randomUUID(),
    override var version: LocalDateTime? = null,

    var announcementId: UUID,

    var accountId: Int,

    var readTime: OffsetDateTime = OffsetDateTime.now(),
) : JpaEntity<UUID>() {

    override fun equalityProperties() = setOf(AnnouncementRead::announcementId, AnnouncementRead::accountId)
}
