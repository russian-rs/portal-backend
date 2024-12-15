package rs.russian.portal.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.ApplicationDto
import rs.russian.portal.application.domain.Application
import rs.russian.portal.application.mapper.ApplicationMapper
import rs.russian.portal.application.repository.ApplicationRepository
import java.util.*

@Service
class ApplicationService(
    private val applicationMapper: ApplicationMapper,
    private val applicationRepository: ApplicationRepository
) {

    @Transactional
    fun create(request: ApplicationDto): Application {
        val email = request.email!!
        val name = request.name!!
        val existByEmail = applicationRepository.findByEmail(email)
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
}
