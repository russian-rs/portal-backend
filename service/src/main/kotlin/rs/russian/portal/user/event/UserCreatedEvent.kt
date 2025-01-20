package rs.russian.portal.user.event

import org.springframework.context.ApplicationEvent

class UserCreatedEvent(val id: Int) : ApplicationEvent(id)
