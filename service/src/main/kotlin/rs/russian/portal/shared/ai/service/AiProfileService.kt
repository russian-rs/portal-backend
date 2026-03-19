package rs.russian.portal.shared.ai.service

import org.slf4j.LoggerFactory
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
            ?: defaultProfile(code)
    }

    private fun defaultProfile(code: AiProfileCode): AiProfile {
        log.warn("AI profile not found or disabled, using default profile for code {}", code)
        return AiProfile(
            code = code,
            systemPrompt = "",
            targetLanguage = "",
            model = null,
            temperature = null,
            enabled = true
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(AiProfileService::class.java)
    }
}
