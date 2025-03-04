package rs.russian.portal.mail.service

import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.file.service.FileService
import rs.russian.portal.file.service.S3Service
import rs.russian.portal.mail.domain.EmailOutbox
import rs.russian.portal.mail.domain.EmailOutboxStatus.CREATED
import rs.russian.portal.mail.domain.EmailOutboxStatus.RETRY
import rs.russian.portal.mail.domain.EmailProperties
import rs.russian.portal.mail.repository.EmailOutboxRepository

@Service
class EmailOutboxService(
    private val s3Service: S3Service,
    private val fileService: FileService,
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
        val helper = MimeMessageHelper(mimeMessage, true)
        helper.setSubject(emailProperties.subject)
        helper.setFrom(emailProperties.from)
        helper.setText(emailProperties.body, true)
        emailProperties.toList.forEach { helper.addTo(it) }
        if (emailProperties.ccList.isNotEmpty()) {
            emailProperties.ccList.forEach { helper.addCc(it) }
        }
        emailProperties.attachments?.let { attachments ->
            attachments.map { attachment -> fileService.getFile(attachment) }
                .forEach { helper.addAttachment(it.name) { s3Service.get(it) } }
        }
        mailSender.send(mimeMessage)
    }

    @Transactional(readOnly = true)
    fun getUnprocessed(): List<EmailOutbox> {
        return emailOutboxRepository.findAllByStatusIn(listOf(CREATED, RETRY))
    }
}
