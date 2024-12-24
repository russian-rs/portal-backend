package rs.russian.portal.mail.scheduler

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import rs.russian.portal.mail.domain.EmailOutboxStatus
import rs.russian.portal.mail.service.EmailOutboxService
import java.time.LocalDateTime

@Component
class EmailOutboxScheduler(
    private val emailOutboxService: EmailOutboxService
) {

    @Scheduled(cron = "\${app.schedulers.email-outbox}")
    @SchedulerLock(
        name = "email_outbox",
        lockAtLeastFor = "PT14S",
        lockAtMostFor = "PT1M"
    )
    fun send() {
        val emails = emailOutboxService.getUnprocessed()
        if (emails.isEmpty()) {
            return
        }
        emails.forEach { email ->
            try {
                emailOutboxService.send(email.properties)
                email.sendTime = LocalDateTime.now()
                email.status = EmailOutboxStatus.OK
                email.errorMessage = null
            } catch (e: Exception) {
                log.error("[EMAIL OUTBOX] Failed to send email", e)
                email.errorMessage = e.message
                if (email.retries < 3) {
                    email.status = EmailOutboxStatus.RETRY
                    email.retries += 1
                } else {
                    email.status = EmailOutboxStatus.ERROR
                }
            } finally {
                emailOutboxService.save(email)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
