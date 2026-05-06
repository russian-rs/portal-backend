package rs.russian.portal.shared.ai.service

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rs.russian.portal.shared.ai.LlmClient
import rs.russian.portal.shared.ai.LlmGenerationRequest
import rs.russian.portal.shared.ai.domain.AiProfile
import rs.russian.portal.shared.ai.domain.AiProfileCode

class TextTranslationServiceTest {

    private lateinit var aiProfileService: AiProfileService
    private lateinit var llmClient: LlmClient
    private lateinit var textTranslationService: TextTranslationService

    @BeforeEach
    fun setup() {
        aiProfileService = mockk()
        llmClient = mockk()
        textTranslationService = TextTranslationService(aiProfileService, llmClient)
    }

    @Test
    fun `translate should return blank text without calling dependencies`() {
        val result = textTranslationService.translate("   ", AiProfileCode.SERBIAN_TRANSLATOR)

        assertEquals("   ", result)
        verify { aiProfileService wasNot Called }
        verify { llmClient wasNot Called }
    }

    @Test
    fun `translate should use active profile and return llm response`() {
        val profile = AiProfile(
            code = AiProfileCode.SERBIAN_TRANSLATOR,
            systemPrompt = "translate to Serbian",
            targetLanguage = "sr",
            model = "gpt-4.1",
            temperature = 0.2,
            enabled = true
        )
        val requestSlot = slot<LlmGenerationRequest>()

        every { aiProfileService.getActiveProfile(AiProfileCode.SERBIAN_TRANSLATOR) } returns profile
        every { llmClient.generateText(capture(requestSlot)) } returns "Preveden tekst"

        val result = textTranslationService.translate("Translated text", AiProfileCode.SERBIAN_TRANSLATOR)

        assertEquals("Preveden tekst", result)
        assertEquals("translate to Serbian", requestSlot.captured.systemPrompt)
        assertEquals("Translated text", requestSlot.captured.userPrompt)
        assertEquals("gpt-4.1", requestSlot.captured.model)
        assertEquals(0.2, requestSlot.captured.temperature)

        verify(exactly = 1) { aiProfileService.getActiveProfile(AiProfileCode.SERBIAN_TRANSLATOR) }
        verify(exactly = 1) { llmClient.generateText(any()) }
    }

    @Test
    fun `translate should return original text when llm client throws`() {
        val profile = AiProfile(
            code = AiProfileCode.SERBIAN_TRANSLATOR,
            systemPrompt = "translate to Serbian",
            targetLanguage = "sr",
            model = "gpt-4.1",
            temperature = 0.2,
            enabled = true
        )

        every { aiProfileService.getActiveProfile(AiProfileCode.SERBIAN_TRANSLATOR) } returns profile
        every { llmClient.generateText(any()) } throws RuntimeException("boom")

        val result = textTranslationService.translate("Original text", AiProfileCode.SERBIAN_TRANSLATOR)

        assertEquals("Original text", result)
        verify(exactly = 1) { aiProfileService.getActiveProfile(AiProfileCode.SERBIAN_TRANSLATOR) }
        verify(exactly = 1) { llmClient.generateText(any()) }
    }
}
