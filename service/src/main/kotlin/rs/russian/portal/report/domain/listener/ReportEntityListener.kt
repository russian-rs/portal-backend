package rs.russian.portal.report.domain.listener

import jakarta.persistence.PostUpdate
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.event.ReportUpdatedEvent

@Component
class ReportEntityListener(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    @PostUpdate
    fun postUpdate(report: Report) {
        report.id?.let {
            applicationEventPublisher.publishEvent(ReportUpdatedEvent(it))
        }
    }
}
