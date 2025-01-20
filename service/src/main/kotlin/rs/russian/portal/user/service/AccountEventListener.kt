package rs.russian.portal.user.service

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import rs.russian.portal.mail.service.EmailService
import rs.russian.portal.user.event.UserCreatedEvent
import rs.russian.portal.user.service.authentik.AuthentikService

@Component
class AccountEventListener(
    private val emailService: EmailService,
    private val accountService: AccountService,
    private val templateEngine: TemplateEngine,
    private val authentikService: AuthentikService
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(fallbackExecution = true)
    fun handleUserCreation(event: UserCreatedEvent) {
        val account = accountService.getAccount(event.id)
        val recoveryLink = authentikService.createRecoveryLink(account)
        val message = templateEngine.process("account_created",
            Context().also { it.setVariables(mapOf("link" to recoveryLink)) })
        emailService.sendCommonEmail(account, "Учетная запись", message)
    }
}
