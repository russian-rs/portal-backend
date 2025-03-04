package rs.russian.portal.maps.scheduler

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import rs.russian.portal.maps.service.GeocodingService
import rs.russian.portal.user.service.AccountService

@Component
class UserCoordinatesScheduler(
    private val accountService: AccountService,
    private val geocodingService: GeocodingService
) {

    @Scheduled(cron = "\${app.schedulers.user-coordinates}")
    //@SchedulerLock(name = "userCoordinates")
    fun update() {
        val users = accountService.findAll()
        users.forEach { user ->
            user.info?.address?.let { address ->
                val coordinates = geocodingService.getCoordinates(address)
                val newInfo = user.info!!
                newInfo.latitude = coordinates?.first
                newInfo.longitude = coordinates?.second
                accountService.updateInfo(user.id!!, newInfo)
            }
        }
    }
}
