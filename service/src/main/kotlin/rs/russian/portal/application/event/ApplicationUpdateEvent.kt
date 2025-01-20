package rs.russian.portal.application.event

import org.springframework.context.ApplicationEvent
import java.util.*

class ApplicationUpdateEvent(val id: UUID) : ApplicationEvent(id)
