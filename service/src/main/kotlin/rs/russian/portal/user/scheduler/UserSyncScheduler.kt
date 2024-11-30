package rs.russian.portal.user.scheduler

import org.springframework.stereotype.Component
import rs.russian.portal.user.service.AuthentikUserService

@Component
class UserSyncScheduler(
    private val authentikUserService: AuthentikUserService
) {

    fun sync() {
        val authentikUsers = authentikUserService.getAllUsers()
    }
}
