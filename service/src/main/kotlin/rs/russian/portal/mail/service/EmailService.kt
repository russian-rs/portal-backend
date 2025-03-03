package rs.russian.portal.mail.service

import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import rs.russian.portal.mail.domain.EmailOutbox
import rs.russian.portal.mail.domain.EmailProperties
import rs.russian.portal.user.domain.Account

@Service
class EmailService(
    private val templateEngine: TemplateEngine,
    private val emailOutboxService: EmailOutboxService
) {

    fun sendCommonEmail(
        to: String,
        subject: String,
        text: String,
        from: String? = null,
        attachments: List<String>? = null
    ) {
        val context = Context().also { it.setVariables(mapOf("text" to text)) }
        val body = templateEngine.process("common_template", context)
        val props = EmailProperties(toList = listOf(to), subject = subject, body = body, attachments = attachments)
        from?.let { props.from = it }
        emailOutboxService.save(EmailOutbox(properties = props))
    }

    fun sendCommonEmail(
        user: Account,
        subject: String,
        text: String,
        from: String? = null,
        attachments: List<String>? = null
    ) {
        sendCommonEmail("${user.fullName} <${user.email}>", subject, text, from, attachments)
    }
}
