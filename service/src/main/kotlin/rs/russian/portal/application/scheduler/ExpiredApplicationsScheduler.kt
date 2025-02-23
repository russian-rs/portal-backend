package rs.russian.portal.application.scheduler

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import rs.russian.portal.application.domain.Application
import rs.russian.portal.application.domain.ApplicationStatus.DENY
import rs.russian.portal.application.domain.ApplicationStatus.DONE
import rs.russian.portal.application.domain.Application_
import rs.russian.portal.application.repository.ApplicationRepository
import rs.russian.portal.application.service.ApplicationService
import rs.russian.portal.shared.jpa.less
import rs.russian.portal.shared.jpa.notContains
import java.time.LocalDateTime

@Component
class ExpiredApplicationsScheduler(
    private val applicationService: ApplicationService,
    private val applicationRepository: ApplicationRepository
) {

    @Scheduled(cron = "\${app.schedulers.expired-applications}")
    @SchedulerLock(name = "expiredApplications")
    fun run() {
        log.info("[SCHEDULER] Denying expired applications")
        val spec: Specification<Application> =
            less<Application, LocalDateTime>(Application_.VERSION, LocalDateTime.now().minusMonths(1))
                .and(notContains(Application_.STATUS, listOf(DONE, DENY)))
        val applications = applicationRepository.findAll(spec, Pageable.unpaged())
        applications.forEach {
            applicationService.save(it.also { it.status = DENY })
        }
        log.info("[SCHEDULER] Denied {} applications", applications.size)
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
