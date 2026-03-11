package rs.russian.portal.shared.ai.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.shared.ai.domain.AiProfile
import rs.russian.portal.shared.ai.domain.AiProfileCode
import rs.russian.portal.shared.ai.repository.AiProfileRepository

@Service
class AiProfileService(
    private val aiProfileRepository: AiProfileRepository
) {

    @Transactional(readOnly = true)
    fun getActiveProfile(code: AiProfileCode): AiProfile {
        return aiProfileRepository.findByCodeAndEnabledTrue(code)
            ?: throw IllegalStateException("AI profile not found or disabled: $code")
    }
}
