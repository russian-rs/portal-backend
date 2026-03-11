package rs.russian.portal.shared.ai

interface LlmClient {
    fun generateText(request: LlmGenerationRequest): String
}
