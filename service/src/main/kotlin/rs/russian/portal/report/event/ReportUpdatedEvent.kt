package rs.russian.portal.report.event

import org.springframework.context.ApplicationEvent
import java.util.*

class ReportUpdatedEvent(val id: UUID) : ApplicationEvent(id)
