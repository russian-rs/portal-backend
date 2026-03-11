package rs.russian.portal.shared.ai

import org.springframework.stereotype.Component

@Component
class NoopLlmClient : LlmClient {
    override fun generateText(request: LlmGenerationRequest): String = request.userPrompt
}
