package rs.russian.portal.shared.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import rs.russian.generated.model.TextTranslationRequest
import rs.russian.portal.shared.ai.domain.AiProfileCode.SERBIAN_TRANSLATOR
import rs.russian.portal.shared.ai.service.TextTranslationService
import rs.russian.portal.shared.exception.GlobalExceptionHandler

class TranslationControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper

    private val textTranslationService: TextTranslationService = mockk()

    @BeforeEach
    fun setUp() {
        val controller = TranslationController(textTranslationService)

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()

        objectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
    }

    @Test
    fun `translateToSerbian should return translated text`() {
        every { textTranslationService.translate("Hello world", SERBIAN_TRANSLATOR) } returns "Zdravo svete"

        mockMvc.post("/translate/serbian") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(TextTranslationRequest(text = "Hello world"))
        }.andExpect {
            status { isOk() }
            content {
                json("""{"text":"Zdravo svete"}""")
            }
        }

        verify(exactly = 1) { textTranslationService.translate("Hello world", SERBIAN_TRANSLATOR) }
    }

    @Test
    fun `translateToSerbian should pass blank text through`() {
        every { textTranslationService.translate("   ", SERBIAN_TRANSLATOR) } returns "   "

        mockMvc.post("/translate/serbian") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(TextTranslationRequest(text = "   "))
        }.andExpect {
            status { isOk() }
            content {
                json("""{"text":"   "}""")
            }
        }

        verify(exactly = 1) { textTranslationService.translate("   ", SERBIAN_TRANSLATOR) }
    }
}
