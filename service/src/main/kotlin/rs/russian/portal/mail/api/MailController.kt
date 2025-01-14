package rs.russian.portal.mail.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.MailApi
import rs.russian.portal.mail.service.EmailService
import rs.russian.portal.shared.enums.UserGroup.ADMIN
import rs.russian.portal.shared.enums.UserGroup.ADMIN_VOLUNTEER
import rs.russian.portal.shared.security.Authorized
import rs.russian.portal.user.service.AccountService

@RestController
class MailController(
    private val emailService: EmailService,
    private val accountService: AccountService
) : MailApi {

    @Authorized(allowed = [ADMIN, ADMIN_VOLUNTEER])
    override fun sendMail(subject: String, body: String, email: String?, username: String?): ResponseEntity<Unit> {
        if (email.isNullOrBlank() && username.isNullOrBlank()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        if (!username.isNullOrBlank()) {
            emailService.sendCommonEmail(accountService.getAccountByLogin(username), subject, body)
        } else {
            emailService.sendCommonEmail(email!!, subject, body)
        }
        return ResponseEntity(HttpStatus.OK)
    }
}
