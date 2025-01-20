package rs.russian.portal.application.event

import org.springframework.context.ApplicationEvent
import java.util.*

class ApplicationCreatedEvent(val id: UUID) : ApplicationEvent(id)
