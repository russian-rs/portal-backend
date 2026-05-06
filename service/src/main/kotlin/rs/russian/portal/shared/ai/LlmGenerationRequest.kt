package rs.russian.portal.shared.ai

data class LlmGenerationRequest(
    val systemPrompt: String,
    val userPrompt: String,
    val model: String? = null,
    val temperature: Double? = null
)
