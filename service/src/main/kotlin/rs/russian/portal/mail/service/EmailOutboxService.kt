package rs.russian.portal.mail.service

import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.mail.domain.EmailOutbox
import rs.russian.portal.mail.domain.EmailOutboxStatus.CREATED
import rs.russian.portal.mail.domain.EmailOutboxStatus.RETRY
import rs.russian.portal.mail.domain.EmailProperties
import rs.russian.portal.mail.repository.EmailOutboxRepository

@Service
class EmailOutboxService(
    private val mailSender: JavaMailSender,
    private val emailOutboxRepository: EmailOutboxRepository
) {

    @Transactional
    fun save(outbox: EmailOutbox): EmailOutbox {
        return emailOutboxRepository.save(outbox)
    }

    @Throws(MailException::class)
    fun send(emailProperties: EmailProperties) {
        val mimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage, "utf-8")
        helper.setSubject(emailProperties.subject)
        helper.setFrom(emailProperties.from)
        helper.setText(emailProperties.body, true)
        emailProperties.toList.forEach { helper.addTo(it) }
        if (emailProperties.ccList.isNotEmpty()) {
            emailProperties.ccList.forEach { helper.addCc(it) }
        }
        mailSender.send(mimeMessage)
    }

    @Transactional(readOnly = true)
    fun getUnprocessed(): List<EmailOutbox> {
        return emailOutboxRepository.findAllByStatusIn(listOf(CREATED, RETRY))
    }
}
