package rs.russian.portal.shared.ai

interface LlmClient {
    fun isAvailable(): Boolean = true
    fun generateText(request: LlmGenerationRequest): String
}
