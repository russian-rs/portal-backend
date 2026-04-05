package rs.russian.portal.shared.ai.repository

import org.springframework.data.jpa.repository.JpaRepository
import rs.russian.portal.shared.ai.domain.AiProfile
import rs.russian.portal.shared.ai.domain.AiProfileCode
import java.util.UUID

interface AiProfileRepository : JpaRepository<AiProfile, UUID> {
    fun findByCodeAndEnabledTrue(code: AiProfileCode): AiProfile?
}
