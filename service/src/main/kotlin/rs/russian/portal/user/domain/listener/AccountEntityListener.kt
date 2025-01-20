package rs.russian.portal.user.domain.listener

import jakarta.persistence.PostPersist
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.event.UserCreatedEvent

@Component
class AccountEntityListener(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    @PostPersist
    fun postCreate(account: Account) {
        account.id?.let {
            applicationEventPublisher.publishEvent(UserCreatedEvent(it))
        }
    }
}
