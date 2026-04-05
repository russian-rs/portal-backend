package rs.russian.portal.shared.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.TranslateApi
import rs.russian.generated.model.TextTranslationRequest
import rs.russian.generated.model.TextTranslationResponse
import rs.russian.portal.shared.ai.domain.AiProfileCode.SERBIAN_TRANSLATOR
import rs.russian.portal.shared.ai.service.TextTranslationService

@RestController
class TranslationController(
    private val textTranslationService: TextTranslationService
) : TranslateApi {

    override fun translateToSerbian(textTranslationRequest: TextTranslationRequest): ResponseEntity<TextTranslationResponse> {
        return ResponseEntity.ok(
            TextTranslationResponse(
                text = textTranslationService.translate(textTranslationRequest.text, SERBIAN_TRANSLATOR)
            )
        )
    }
}
