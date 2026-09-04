package rs.russian.portal.application.domain.listener

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import rs.russian.portal.application.domain.Application
import rs.russian.portal.application.domain.ApplicationStatus.*
import rs.russian.portal.application.event.ApplicationUpdateEvent

class ApplicationEntityListenerTest {
    private val publisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val listener = ApplicationEntityListener(publisher)

    @Test
    fun `assignment and ordinary edits of DONE application do not repeat completion effects`() {
        val application = Application(email = "test@example.com", name = "Applicant", status = DONE)
        listener.postLoad(application)
        application.assignee = "employee"
        listener.postUpdate(application)
        application.comment = "New comment"
        listener.postUpdate(application)
        verify(exactly = 0) { publisher.publishEvent(any<ApplicationUpdateEvent>()) }
    }

    @Test
    fun `a real status transition publishes once and refreshes snapshot`() {
        val application = Application(email = "test@example.com", name = "Applicant")
        listener.postLoad(application)
        application.status = DONE
        listener.postUpdate(application)
        application.assignee = "employee"
        listener.postUpdate(application)
        verify(exactly = 1) { publisher.publishEvent(any<ApplicationUpdateEvent>()) }
    }
}
