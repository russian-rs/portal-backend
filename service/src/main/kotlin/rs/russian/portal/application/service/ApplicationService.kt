package rs.russian.portal.application.service

import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.ApplicationDto
import rs.russian.generated.model.PageRequest
import rs.russian.portal.application.domain.Application
import rs.russian.portal.application.domain.ApplicationStatus.DENY
import rs.russian.portal.application.domain.ApplicationStatus.DONE
import rs.russian.portal.application.domain.ApplicationType
import rs.russian.portal.application.domain.specification.searchSpecification
import rs.russian.portal.application.mapper.ApplicationMapper
import rs.russian.portal.application.repository.ApplicationRepository
import rs.russian.portal.shared.jpa.convert
import rs.russian.portal.user.service.AccountService
import java.util.*

@Service
class ApplicationService(
    private val accountService: AccountService,
    private val applicationMapper: ApplicationMapper,
    private val applicationRepository: ApplicationRepository
) {

    @Transactional
    fun create(request: ApplicationDto): Application {
        val email = request.email!!
        val name = request.name!!
        val existByEmail = applicationRepository.findByEmailAndStatusNotIn(email, listOf(DONE, DENY))
        if (existByEmail.isPresent) {
            return existByEmail.get()
        }
        if (!request.passport.isNullOrBlank()) {
            val existByEmailAndPassport = applicationRepository.findByEmailAndPassport(email, request.passport!!)
            if (existByEmailAndPassport.isPresent) {
                return existByEmailAndPassport.get()
            }
        }
        val application = Application(email = email, name = name)
        applicationMapper.map(request, application)
        val existUser = accountService.findAccountByEmail(email)
        if (existUser != null) {
            application.type = ApplicationType.PROLONGATION
        }
        return applicationRepository.save(application)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): Application {
        return applicationRepository.findById(id).orElseThrow()
    }

    @Transactional(readOnly = true)
    fun findByEmail(email: String): Application {
        return applicationRepository.findByEmail(email).orElseThrow()
    }

    @Transactional(readOnly = true)
    fun getAll(searchQuery: String?, pageRequest: PageRequest): Page<Application> {
        val specification = searchSpecification(searchQuery)
        return applicationRepository.findAll(specification, convert(pageRequest))
    }

    @Transactional
    fun update(applicationDto: ApplicationDto): Application {
        val application = get(applicationDto.id)
        applicationMapper.update(applicationDto, application)
        return applicationRepository.save(application)
    }

    @Transactional
    fun save(application: Application): Application {
        return applicationRepository.save(application)
    }
}
