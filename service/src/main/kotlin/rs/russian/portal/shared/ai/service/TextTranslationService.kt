package rs.russian.portal.shared.ai.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import rs.russian.portal.shared.ai.LlmClient
import rs.russian.portal.shared.ai.LlmGenerationRequest
import rs.russian.portal.shared.ai.domain.AiProfileCode

@Service
class TextTranslationService(
    private val aiProfileService: AiProfileService,
    private val llmClient: LlmClient
) {

    fun translate(text: String, profileCode: AiProfileCode): String {
        if (text.isBlank()) {
            return text
        }

        return runCatching {
            val profile = aiProfileService.getActiveProfile(profileCode)
            llmClient.generateText(
                LlmGenerationRequest(
                    systemPrompt = profile.systemPrompt,
                    userPrompt = text,
                    model = profile.model,
                    temperature = profile.temperature
                )
            )
        }.getOrElse { ex ->
            log.error("Text translation failed for profile {}", profileCode, ex)
            text
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TextTranslationService::class.java)
    }
}
