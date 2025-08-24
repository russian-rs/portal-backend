package rs.russian.portal.mail.domain

import jakarta.persistence.*
import jakarta.persistence.EnumType.STRING
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes.JSON
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDateTime
import java.util.*

@Entity
class EmailOutbox(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    override var id: UUID? = UUID.randomUUID(),
    override var version: LocalDateTime? = null,

    @Enumerated(STRING)
    var status: EmailOutboxStatus = EmailOutboxStatus.CREATED,

    @JdbcTypeCode(JSON)
    var properties: EmailProperties,

    var errorMessage: String? = null,
    var retries: Int = 0,
    var createTime: LocalDateTime = LocalDateTime.now(),
    var sendTime: LocalDateTime? = null

) : JpaEntity<UUID>() {

    override fun equalityProperties() = setOf(EmailOutbox::id)
}
