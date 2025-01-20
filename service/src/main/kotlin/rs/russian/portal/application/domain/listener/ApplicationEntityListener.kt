package rs.russian.portal.application.domain.listener

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

    @PostPersist
    fun postPersist(application: Application) {
        application.id?.let {
            applicationEventPublisher.publishEvent(ApplicationCreatedEvent(it))
        }
    }

    @PostUpdate
    fun postUpdate(application: Application) {
        application.id?.let {
            applicationEventPublisher.publishEvent(ApplicationUpdateEvent(it))
        }
    }
}
