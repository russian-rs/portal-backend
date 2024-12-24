package rs.russian.portal.shared.api

import com.digitalsanctuary.cf.turnstile.dto.TurnstileResponse
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("local")
@RequestMapping("/turnstile")
class MockTurnstileController {

    @PostMapping
    fun validate(): ResponseEntity<TurnstileResponse> {
        return ResponseEntity.ok(TurnstileResponse().also { it.isSuccess = true })
    }
}
