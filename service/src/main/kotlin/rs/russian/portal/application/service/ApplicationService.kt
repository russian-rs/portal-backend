package rs.russian.portal.application.service

import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.ApplicationDto
import rs.russian.generated.model.ApplicationsFilter
import rs.russian.generated.model.NoteDto
import rs.russian.generated.model.PageRequest
import rs.russian.portal.application.domain.Application
import rs.russian.portal.application.domain.ApplicationStatus.DENY
import rs.russian.portal.application.domain.ApplicationStatus.DONE
import rs.russian.portal.application.domain.ApplicationType
import rs.russian.portal.application.domain.specification.searchSpecification
import rs.russian.portal.application.mapper.ApplicationMapper
import rs.russian.portal.application.repository.ApplicationRepository
import rs.russian.portal.note.domain.Note
import rs.russian.portal.note.domain.enums.EntityType
import rs.russian.portal.note.service.NoteService
import rs.russian.portal.shared.exception.InvalidRequestException
import rs.russian.portal.shared.exception.NotAuthorizedException
import rs.russian.portal.shared.jpa.convert
import rs.russian.portal.shared.security.currentUserLogin
import rs.russian.portal.user.service.AccountService
import java.util.*

@Service
class ApplicationService(
    private val noteService: NoteService,
    private val accountService: AccountService,
    private val applicationMapper: ApplicationMapper,
    private val applicationRepository: ApplicationRepository,
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
            val existByEmailAndPassport = applicationRepository.findByEmailAndPassportAndStatusNotIn(
                email,
                request.passport!!,
                listOf(DONE, DENY)
            )
            if (existByEmailAndPassport.isPresent) {
                return existByEmailAndPassport.get()
            }
        }
        val application = Application(email = email, name = name)
        applicationMapper.toEntity(request, application)
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
    fun getAll(searchQuery: String?, pageRequest: PageRequest, filter: ApplicationsFilter?): Page<Application> {
        val specification = searchSpecification(searchQuery, filter)
        return applicationRepository.findAll(specification, convert(pageRequest))
    }

    @Transactional
    fun update(applicationDto: ApplicationDto): Application {
        val application = get(applicationDto.id)
        applicationMapper.update(applicationDto, application)
        if (application.status == DONE && application.contractFrom == null) {
            throw InvalidRequestException("Contract dates not specified")
        }
        return applicationRepository.save(application)
    }

    @Transactional
    fun save(application: Application): Application {
        return applicationRepository.save(application)
    }

    @Transactional
    fun addNote(applicationId: UUID, noteDto: NoteDto): Note {
        val application = get(applicationId)
        val currentAccount = accountService.getAccountByLogin(currentUserLogin() ?: throw NotAuthorizedException())
        val note = noteService.save(
            Note(
                createdBy = currentAccount.username,
                entityId = applicationId,
                entityType = EntityType.APPLICATION,
                text = noteDto.text
            )
        )
        application.notes.add(note)
        return note
    }
}
