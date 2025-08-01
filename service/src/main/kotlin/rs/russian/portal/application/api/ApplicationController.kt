package rs.russian.portal.application.api

import com.digitalsanctuary.cf.turnstile.service.TurnstileValidationService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.ApplicationApi
import rs.russian.generated.model.*
import rs.russian.portal.application.mapper.ApplicationMapper
import rs.russian.portal.application.service.ApplicationService
import rs.russian.portal.shared.exception.CaptchaInvalidException
import rs.russian.portal.shared.jpa.convert
import rs.russian.portal.shared.security.Authorized
import rs.russian.portal.user.domain.enums.UserGroup.ADMIN_VOLUNTEER
import rs.russian.portal.user.domain.enums.UserGroup.INTERVIEWER
import java.util.*

@RestController
class ApplicationController(
    private val applicationMapper: ApplicationMapper,
    private val applicationService: ApplicationService,
    private val captchaService: TurnstileValidationService,
    private val httpServletRequest: HttpServletRequest
) : ApplicationApi {

    override fun createApplication(
        captchaToken: String,
        applicationDto: ApplicationDto,
    ): ResponseEntity<ApplicationStatusDto> {
        val captchaValid = captchaService.validateTurnstileResponse(
            captchaToken,
            captchaService.getClientIpAddress(httpServletRequest)
        )
        if (!captchaValid) {
            throw CaptchaInvalidException()
        }
        val application = applicationService.create(applicationDto)
        return ResponseEntity.ok(applicationMapper.mapStatus(application))
    }

    override fun getApplicationStatus(id: UUID): ResponseEntity<ApplicationStatusDto> {
        val application = applicationService.get(id)
        return ResponseEntity.ok(applicationMapper.mapStatus(application))
    }

    override fun searchApplicationByEmail(email: String): ResponseEntity<ApplicationStatusDto> {
        val application = applicationService.findByEmail(email)
        return ResponseEntity.ok(applicationMapper.mapStatus(application))
    }

    @Authorized(allowed = [ADMIN_VOLUNTEER, INTERVIEWER])
    override fun getApplication(id: UUID): ResponseEntity<ApplicationDto> {
        val application = applicationService.get(id)
        return ResponseEntity.ok(applicationMapper.toDto(application))
    }

    @Authorized(allowed = [ADMIN_VOLUNTEER, INTERVIEWER])
    override fun getApplications(
        pageRequest: PageRequest,
        searchQuery: String?,
        applicationsFilter: ApplicationsFilter?
    ): ResponseEntity<ApplicationPageResponse> {
        val page = applicationService.getAll(searchQuery, pageRequest, applicationsFilter)
        return ResponseEntity.ok(
            ApplicationPageResponse(
                page = convert(page),
                content = page.map { applicationMapper.toDto(it) }.toMutableList()
            )
        )
    }

    @Authorized(allowed = [ADMIN_VOLUNTEER, INTERVIEWER])
    override fun updateApplication(applicationDto: ApplicationDto): ResponseEntity<ApplicationDto> {
        val application = applicationService.update(applicationDto)
        return ResponseEntity.ok(applicationMapper.toDto(application))
    }

}
