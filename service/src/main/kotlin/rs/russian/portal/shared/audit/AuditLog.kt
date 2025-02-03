package rs.russian.portal.shared.audit

import jakarta.persistence.*
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.GenerationType.IDENTITY
import java.time.LocalDateTime

@Entity
@Table(name = "audit")
data class AuditLog(

    @Id
    @GeneratedValue(strategy = IDENTITY)
    val id: Long? = null,

    val timestamp: LocalDateTime = LocalDateTime.now(),

    val userLogin: String = "system",

    val entityType: String,

    val entityId: String?,

    @Enumerated(STRING)
    val operation: AuditOperation,

    val data: String,
)
