package rs.russian.portal.application.domain.listener

import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.PostUpdate
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import rs.russian.portal.application.domain.Application
import rs.russian.portal.application.event.ApplicationCreatedEvent
import rs.russian.portal.application.event.ApplicationUpdateEvent

@Component
class ApplicationEntityListener(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    @PostLoad
    fun postLoad(application: Application) {
        application.capturePersistedStatus()
    }

    @PostPersist
    fun postPersist(application: Application) {
        application.capturePersistedStatus()
        application.id?.let {
            applicationEventPublisher.publishEvent(ApplicationCreatedEvent(it))
        }
    }

    @PostUpdate
    fun postUpdate(application: Application) {
        // Assignment and ordinary edits must not repeat DONE processing (e.g. prolongation contracts).
        if (!application.capturePersistedStatus()) return
        application.id?.let {
            applicationEventPublisher.publishEvent(ApplicationUpdateEvent(it))
        }
    }
}
