package rs.russian.portal.shared.ai

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
@ConditionalOnExpression(
    "T(org.springframework.util.StringUtils).hasText('\${spring.ai.openai.api-key:}') || " +
        "T(org.springframework.util.StringUtils).hasText('\${spring.ai.openai.chat.api-key:}')"
)
class SpringAiLlmClient(
    chatClientBuilder: ChatClient.Builder
) : LlmClient {

    private val chatClient = chatClientBuilder.build()
    override fun isAvailable(): Boolean = true

    override fun generateText(request: LlmGenerationRequest): String {
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
