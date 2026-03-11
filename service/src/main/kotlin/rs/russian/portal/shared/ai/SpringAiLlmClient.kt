package rs.russian.portal.shared.ai

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SpringAiLlmClient(
    chatClientBuilder: ChatClient.Builder,
    @param:Value("\${spring.ai.openai.api-key:}")
    private val apiKey: String
) : LlmClient {

    private val chatClient = chatClientBuilder.build()

    override fun generateText(request: LlmGenerationRequest): String {
        if (apiKey.isBlank()) {
            throw IllegalStateException("LLM API key is not configured")
        }

        val promptSpec = chatClient.prompt()
            .system(request.systemPrompt)
            .user(request.userPrompt)

        val response = if (request.model != null || request.temperature != null) {
            promptSpec.options(
                OpenAiChatOptions.builder()
                    .apply {
                        request.model?.let { model(it) }
                        request.temperature?.let { temperature(it) }
                    }
                    .build()
            ).call().content()
        } else {
            promptSpec.call().content()
        }

        return response?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("LLM returned an empty response")
    }
}
