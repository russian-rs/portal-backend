package rs.russian.portal.shared.ai.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "ai_profile")
class AiProfile(
    @Id
    @Column(name = "id")
    override var id: UUID? = UUID.randomUUID(),

    override var version: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, unique = true)
    var code: AiProfileCode,

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    var systemPrompt: String,

    @Column(name = "target_language", nullable = false, length = 128)
    var targetLanguage: String,

    @Column(name = "model", length = 255)
    var model: String? = null,

    @Column(name = "temperature")
    var temperature: Double? = null,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true
) : JpaEntity<UUID>() {
    override fun equalityProperties() = setOf(AiProfile::code)
}
