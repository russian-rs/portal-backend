package rs.russian.portal.application.api

import com.digitalsanctuary.cf.turnstile.service.TurnstileValidationService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import rs.russian.generated.model.ApplicationDto
import rs.russian.portal.application.mapper.ApplicationMapper
import rs.russian.portal.application.service.ApplicationService
import rs.russian.portal.note.mapper.NoteMapper
import rs.russian.portal.shared.exception.GlobalExceptionHandler
import java.util.*

class ApplicationValidationTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper

    private val noteMapper: NoteMapper = mockk(relaxed = true)
    private val applicationMapper: ApplicationMapper = mockk(relaxed = true)
    private val applicationService: ApplicationService = mockk(relaxed = true)
    private val httpServletRequest: HttpServletRequest = mockk(relaxed = true)
    private val captchaService: TurnstileValidationService = mockk {
        every { validateTurnstileResponse(any(), any()) } returns true
        every { getClientIpAddress(any()) } returns "127.0.0.1"
    }

    @BeforeEach
    fun setUp() {
        val controller = ApplicationController(
            noteMapper = noteMapper,
            applicationMapper = applicationMapper,
            applicationService = applicationService,
            captchaService = captchaService,
            httpServletRequest = httpServletRequest
        )

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()

        objectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
    }

    @Test
    fun `createApplication should return 400 when email exceeds max length`() {
        val longEmail = "a".repeat(100) + "@example.com" // 112 chars, max is 100
        val dto = ApplicationDto(
            id = UUID.randomUUID(),
            email = longEmail,
            name = "Test Name"
        )

        mockMvc.post("/application/create") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(dto)
            param("captchaToken", "valid-token")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `createApplication should return 400 when email format is invalid`() {
        val dto = ApplicationDto(
            id = UUID.randomUUID(),
            email = "invalid-email",
            name = "Test Name"
        )

        mockMvc.post("/application/create") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(dto)
            param("captchaToken", "valid-token")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `createApplication should return 400 when name exceeds max length`() {
        val dto = ApplicationDto(
            id = UUID.randomUUID(),
            email = "test@example.com",
            name = "A".repeat(101) // max is 100
        )

        mockMvc.post("/application/create") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(dto)
            param("captchaToken", "valid-token")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `createApplication should return 400 when bio exceeds max length`() {
        val dto = ApplicationDto(
            id = UUID.randomUUID(),
            email = "test@example.com",
            name = "Test Name",
            bio = "A".repeat(1001) // max is 1000
        )

        mockMvc.post("/application/create") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(dto)
            param("captchaToken", "valid-token")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `createApplication should return 400 when goal exceeds max length`() {
        val dto = ApplicationDto(
            id = UUID.randomUUID(),
            email = "test@example.com",
            name = "Test Name",
            goal = "A".repeat(2001) // max is 2000
        )

        mockMvc.post("/application/create") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(dto)
            param("captchaToken", "valid-token")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `createApplication should return 400 when address exceeds max length`() {
        val dto = ApplicationDto(
            id = UUID.randomUUID(),
            email = "test@example.com",
            name = "Test Name",
            address = "A".repeat(201) // max is 200
        )

        mockMvc.post("/application/create") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(dto)
            param("captchaToken", "valid-token")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `createApplication should return 400 when postalCode exceeds max length`() {
        val dto = ApplicationDto(
            id = UUID.randomUUID(),
            email = "test@example.com",
            name = "Test Name",
            postalCode = "A".repeat(21) // max is 20
        )

        mockMvc.post("/application/create") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(dto)
            param("captchaToken", "valid-token")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `createApplication should return 400 when phone exceeds max length`() {
        val dto = ApplicationDto(
            id = UUID.randomUUID(),
            email = "test@example.com",
            name = "Test Name",
            phone = "1".repeat(31) // max is 30
        )

        mockMvc.post("/application/create") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(dto)
            param("captchaToken", "valid-token")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `createApplication should accept valid application data`() {
        val dto = ApplicationDto(
            id = UUID.randomUUID(),
            email = "test@example.com",
            name = "Valid Name",
            bio = "Valid bio",
            goal = "Valid goal",
            address = "Valid address",
            postalCode = "11000",
            phone = "+381641234567"
        )

        // Mock the service to return an application
        every { applicationService.create(any()) } returns mockk(relaxed = true)

        mockMvc.post("/application/create") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(dto)
            param("captchaToken", "valid-token")
        }.andExpect {
            status { isOk() }
        }
    }
}
